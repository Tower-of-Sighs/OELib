package cc.sighs.oelib.neoforge.registry;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.registry.RegisterSupplier;
import cc.sighs.oelib.registry.action.*;
import cc.sighs.oelib.registry.api.CreativeTabModifyCallback;
import cc.sighs.oelib.registry.api.CreativeTabOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings({"unused", "unchecked"})
public final class ServerRegistrationHandler {

    private static final Map<ResourceKey<?>, List<ListenEntry<?>>> ENTRY_LISTENERS = new ConcurrentHashMap<>();
    private static final Map<Identifier, List<Supplier<ItemStack>>> TAB_APPENDS = new ConcurrentHashMap<>();
    private static final Map<Identifier, List<CreativeTabModifyCallback>> TAB_MODIFIERS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<?>, List<RegisterSupplier<?>>> REGISTERED_ENTRIES = new ConcurrentHashMap<>();
    private static final List<CommandRegisterAction> SERVER_COMMANDS = new CopyOnWriteArrayList<>();

    private static final Map<Supplier<? extends EntityType<? extends LivingEntity>>, Supplier<AttributeSupplier.Builder>> ATTRIBUTES = new ConcurrentHashMap<>();
    private static final List<SpawnPlacementAction<?>> SPAWN_PLACEMENTS = new CopyOnWriteArrayList<>();
    private static final Map<ItemLike, Integer> FUEL_TIMES = new ConcurrentHashMap<>();

    private static void putFuelTime(int time, ItemLike item) {
        FUEL_TIMES.put(item, time);
    }

    public void perform(RegistrationAction action) {
        if (action instanceof RegistryBatchAction<?> batchRaw) {
            performRegistryBatch((RegistryBatchAction<Object>) batchRaw);
            return;
        }

        if (action instanceof ListenAction<?> laRaw) {
            var la = (ListenAction<Object>) laRaw;
            ENTRY_LISTENERS.computeIfAbsent(la.registryKey(), k -> new CopyOnWriteArrayList<>())
                    .add(new ListenEntry<>(la.supplier(), la.callback()));
            return;
        }

        if (action instanceof CreativeTabAppendStackAction(
                ResourceKey<CreativeModeTab> tab, Supplier<ItemStack> item
        )) {
            TAB_APPENDS.computeIfAbsent(tab.identifier(), k -> new CopyOnWriteArrayList<>()).add(item);
            return;
        }

        if (action instanceof CreativeTabModifyAction(
                ResourceKey<CreativeModeTab> tab, CreativeTabModifyCallback callback
        )) {
            TAB_MODIFIERS.computeIfAbsent(tab.identifier(), k -> new CopyOnWriteArrayList<>()).add(callback);
            return;
        }

        if (action instanceof CommandRegisterAction cra) {
            if (!cra.clientOnly()) {
                SERVER_COMMANDS.add(cra);
            }
            return;
        }

        if (action instanceof EntityAttributeAction(
                Supplier<? extends EntityType<? extends LivingEntity>> type,
                Supplier<AttributeSupplier.Builder> attributes
        )) {
            ATTRIBUTES.put(type, attributes);
            return;
        }

        if (action instanceof SpawnPlacementAction<?> spa) {
            SPAWN_PLACEMENTS.add(spa);
            return;
        }

        if (action instanceof FuelAction(int time, Supplier<? extends ItemLike>[] items)) {
            for (var s : items) {
                if (s instanceof RegisterSupplier<?> rs) {
                    @SuppressWarnings("unchecked")
                    RegisterSupplier<? extends ItemLike> itemSupplier = (RegisterSupplier<? extends ItemLike>) rs;
                    itemSupplier.listen(item -> putFuelTime(time, item));
                    continue;
                }
                putFuelTime(time, s.get());
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private <T> void performRegistryBatch(RegistryBatchAction<T> batch) {
        var key = batch.registryKey();
        var modid = batch.modid();
        var dr = DeferredRegister.create(key, modid);
        dr.register(ModLoadingContext.get().getActiveContainer().getEventBus());
        for (RegisterSupplier<? extends T> entry : batch.entries()) {
            Supplier<T> supplier = (Supplier<T>) entry.getCreator();
            var ro = dr.register(entry.id().getPath(), supplier);
            ((RegisterSupplier<T>) entry).setGetter(ro);
        }
        REGISTERED_ENTRIES.computeIfAbsent(key, _k -> new CopyOnWriteArrayList<>()).addAll(batch.entries());
    }

    private record ListenEntry<T>(RegisterSupplier<? extends T> supplier, Consumer<? super T> callback) {
    }

    @EventBusSubscriber(modid = OELib.MODID)
    public static class ModEventHooks {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onNotifyRegistered(RegisterEvent event) {
            var entries = REGISTERED_ENTRIES.remove(event.getRegistryKey());
            if (entries != null) {
                for (var e : entries) {
                    Object obj = e.get();
                    ((RegisterSupplier<Object>) e).bindInstance(obj);
                }
            }
            var list = ENTRY_LISTENERS.remove(event.getRegistryKey());
            if (list != null) {
                for (var leRaw : list) {
                    var le = (ListenEntry<Object>) leRaw;
                    le.callback.accept(le.supplier.get());
                }
            }
        }

        @SubscribeEvent
        public static void onBuildCreative(BuildCreativeModeTabContentsEvent event) {
            var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab());
            if (id == null) return;
            var items = TAB_APPENDS.get(id);
            if (items != null) {
                for (var s : items) {
                    event.accept(s.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            }
            var modifiers = TAB_MODIFIERS.get(id);
            if (modifiers != null) {
                var out = new CreativeTabOutput() {
                    @Override
                    public void acceptAfter(ItemStack after, ItemStack stack, CreativeModeTab.TabVisibility v) {
                        event.insertAfter(after, stack, v);
                    }

                    @Override
                    public void acceptBefore(ItemStack before, ItemStack stack, CreativeModeTab.TabVisibility v) {
                        event.insertBefore(before, stack, v);
                    }

                    @Override
                    public void accept(ItemStack stack, CreativeModeTab.TabVisibility v) {
                        event.accept(stack, v);
                    }
                };
                for (var m : modifiers) {
                    m.accept(event.getFlags(), out, event.hasPermissions());
                }
            }
        }

        @SubscribeEvent
        public static void onEntityAttributes(EntityAttributeCreationEvent event) {
            for (var e : ATTRIBUTES.entrySet()) {
                event.put(e.getKey().get(), e.getValue().get().build());
            }
        }

        @SubscribeEvent
        public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
            for (var spaRaw : SPAWN_PLACEMENTS) {
                var spa = (SpawnPlacementAction<Mob>) spaRaw;
                event.register(spa.type().get(), spa.placement(), spa.heightmap(), spa.predicate(), RegisterSpawnPlacementsEvent.Operation.OR);
            }
        }

        @SubscribeEvent
        public static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event) {
            var stack = event.getItemStack();
            if (stack.isEmpty()) return;
            int time = FUEL_TIMES.getOrDefault(stack.getItem(), Integer.MIN_VALUE);
            if (time != Integer.MIN_VALUE) event.setBurnTime(Math.max(time, 0));
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            for (var cra : SERVER_COMMANDS) {
                cra.registrar().register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
            }
        }
    }
}
