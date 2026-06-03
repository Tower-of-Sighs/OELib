package cc.sighs.oelib.registry.fabric;

import cc.sighs.oelib.registry.RegisterSupplier;
import cc.sighs.oelib.registry.action.*;
import cc.sighs.oelib.registry.api.CreativeTabModifyCallback;
import cc.sighs.oelib.registry.api.CreativeTabOutput;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public final class ServerRegistrationHandler {

    private static final Map<ResourceLocation, List<Supplier<ItemStack>>> TAB_APPENDS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<CreativeTabModifyCallback>> TAB_MODIFIERS = new ConcurrentHashMap<>();
    private static final Map<RegisterSupplier<?>, List<Consumer<?>>> ENTRY_LISTENERS = new ConcurrentHashMap<>();
    private static final List<CommandRegisterAction> SERVER_COMMANDS = new CopyOnWriteArrayList<>();
    private static volatile boolean creativeTabEventsInstalled = false;
    private static volatile boolean serverCommandEventsInstalled = false;

    @SuppressWarnings("unchecked")
    public void perform(RegistrationAction action) {
        if (action instanceof RegistryBatchAction<?>) {
            RegistryBatchAction<Object> batchRaw = (RegistryBatchAction<Object>) action;
            performRegistryBatch(batchRaw);
            return;
        }

        if (action instanceof CommandRegisterAction cra) {
            if (!cra.clientOnly()) {
                ensureServerCommandEvents();
                SERVER_COMMANDS.add(cra);
            }
            return;
        }

        if (action instanceof ListenAction<?>) {
            ListenAction<Object> la = (ListenAction<Object>) action;
            var raw = BuiltInRegistries.REGISTRY.get(la.registryKey().location());
            if (raw != null) {
                var reg = (Registry<Object>) raw;
                var obj = reg.get(la.supplier().id());
                if (obj != null) {
                    if (!la.supplier().isPresent()) {
                        la.supplier().bindInstance(obj);
                    }
                    la.callback().accept(obj);
                    return;
                }
            }
            ENTRY_LISTENERS.computeIfAbsent(la.supplier(), k -> new CopyOnWriteArrayList<>()).add(la.callback());
            return;
        }

        if (action instanceof CreativeTabAppendStackAction(
                ResourceKey<CreativeModeTab> tabKey, Supplier<ItemStack> item
        )) {
            ensureCreativeTabEventRegistered();
            TAB_APPENDS.computeIfAbsent(tabKey.location(), k -> new CopyOnWriteArrayList<>()).add(item);
            return;
        }

        if (action instanceof CreativeTabModifyAction(
                ResourceKey<CreativeModeTab> tabKey, CreativeTabModifyCallback callback
        )) {
            ensureCreativeTabEventRegistered();
            TAB_MODIFIERS.computeIfAbsent(tabKey.location(), k -> new CopyOnWriteArrayList<>()).add(callback);
            return;
        }

        if (action instanceof FuelAction(int time, Supplier<? extends ItemLike>[] items)) {
            for (var s : items) {
                if (s instanceof RegisterSupplier<?> rs) {
                    @SuppressWarnings("unchecked")
                    RegisterSupplier<? extends ItemLike> itemSupplier = (RegisterSupplier<? extends ItemLike>) rs;
                    itemSupplier.listen(item -> {
                        if (time >= 0) {
                            FuelRegistry.INSTANCE.add(item, time);
                        } else {
                            FuelRegistry.INSTANCE.remove(item);
                        }
                    });
                    continue;
                }
                ItemLike item = s.get();
                if (time >= 0) {
                    FuelRegistry.INSTANCE.add(item, time);
                } else {
                    FuelRegistry.INSTANCE.remove(item);
                }
            }
        }

        if (action instanceof EntityAttributeAction(
                Supplier<? extends EntityType<? extends LivingEntity>> type,
                Supplier<AttributeSupplier.Builder> attributes
        )) {
            FabricDefaultAttributeRegistry.register(type.get(), attributes.get());
            return;
        }

        if (action instanceof SpawnPlacementAction<?> spaRaw) {
            var spa = (SpawnPlacementAction<Mob>) spaRaw;
            SpawnPlacements.register(spa.type().get(), spa.placement(), spa.heightmap(), spa.predicate());
            return;
        }

        if (action instanceof TradeVillagerAction(
                VillagerProfession profession, int level, VillagerTrades.ItemListing[] trades
        )) {
            TradeOfferHelper.registerVillagerOffers(profession, level, list -> Collections.addAll(list, trades));
            return;
        }

        if (action instanceof TradeWandererAction(boolean rare, VillagerTrades.ItemListing[] trades)) {
            TradeOfferHelper.registerWanderingTraderOffers(rare ? 2 : 1, list -> Collections.addAll(list, trades));
        }
    }

    private <T> void performRegistryBatch(RegistryBatchAction<T> batch) {
        var rawRegistry = BuiltInRegistries.REGISTRY.get(batch.registryKey().location());
        if (rawRegistry == null) {
            throw new IllegalStateException("Registry not found for key: " + batch.registryKey().location());
        }
        var registry = (Registry<T>) rawRegistry;
        for (RegisterSupplier<? extends T> entry : batch.entries()) {
            T instance = ((RegisterSupplier<T>) entry).getCreator().get();
            Registry.register(registry, entry.id(), instance);
            ((RegisterSupplier<T>) entry).bindInstance(instance);
            var ls = ENTRY_LISTENERS.remove(entry);
            if (ls != null) {
                for (var c : ls) ((Consumer<T>) c).accept(instance);
            }
        }
    }

    private void ensureCreativeTabEventRegistered() {
        if (!creativeTabEventsInstalled) {
            synchronized (TAB_APPENDS) {
                if (!creativeTabEventsInstalled) {
                    ItemGroupEvents.MODIFY_ENTRIES_ALL.register((tab, entries) -> {
                        var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                        if (id == null) return;
                        var items = TAB_APPENDS.get(id);
                        if (items != null) {
                            for (var s : items) {
                                entries.accept(s.get());
                            }
                        }
                        var modifiers = TAB_MODIFIERS.get(id);
                        if (modifiers != null) {
                            var out = new CreativeTabOutput() {
                                @Override
                                public void acceptAfter(ItemStack after, ItemStack stack, CreativeModeTab.TabVisibility v) {
                                    entries.addAfter(after, List.of(stack), v);
                                }

                                @Override
                                public void acceptBefore(ItemStack before, ItemStack stack, CreativeModeTab.TabVisibility v) {
                                    entries.addBefore(before, List.of(stack), v);
                                }

                                @Override
                                public void accept(ItemStack stack, CreativeModeTab.TabVisibility v) {
                                    entries.accept(stack, v);
                                }
                            };
                            for (var m : modifiers) {
                                m.accept(entries.getEnabledFeatures(), out, entries.shouldShowOpRestrictedItems());
                            }
                        }
                    });
                    creativeTabEventsInstalled = true;
                }
            }
        }
    }

    private void ensureServerCommandEvents() {
        if (!serverCommandEventsInstalled) {
            synchronized (SERVER_COMMANDS) {
                if (!serverCommandEventsInstalled) {
                    CommandRegistrationCallback.EVENT.register(this::handleServerCommandRegistration);
                    serverCommandEventsInstalled = true;
                }
            }
        }
    }

    private void handleServerCommandRegistration(CommandDispatcher<CommandSourceStack> dispatcher,
                                                 CommandBuildContext context,
                                                 Commands.CommandSelection environment) {
        for (var cra : SERVER_COMMANDS) {
            cra.registrar().register(dispatcher, context, environment);
        }
    }
}