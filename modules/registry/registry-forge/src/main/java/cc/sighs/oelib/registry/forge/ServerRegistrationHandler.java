package cc.sighs.oelib.registry.forge;

import cc.sighs.oelib.registry.OELibRegistry;
import cc.sighs.oelib.registry.RegisterSupplier;
import cc.sighs.oelib.registry.action.*;
import cc.sighs.oelib.registry.api.CreativeTabModifyCallback;
import cc.sighs.oelib.registry.api.CreativeTabOutput;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@SuppressWarnings({"unused", "unchecked"})
public final class ServerRegistrationHandler {

    private static final Map<ResourceLocation, List<Supplier<ItemStack>>> TAB_APPENDS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<CreativeTabModifyCallback>> TAB_MODIFIERS = new ConcurrentHashMap<>();
    private static final List<CommandRegisterAction> SERVER_COMMANDS = new CopyOnWriteArrayList<>();

    private static final Map<Supplier<? extends EntityType<? extends LivingEntity>>, Supplier<AttributeSupplier.Builder>> ATTRIBUTES = new ConcurrentHashMap<>();
    private static final List<SpawnPlacementAction<?>> SPAWN_PLACEMENTS = new CopyOnWriteArrayList<>();
    private static final Map<VillagerProfession, Int2ObjectMap<List<VillagerTrades.ItemListing>>> VILLAGER_TRADES = new ConcurrentHashMap<>();
    private static final Int2ObjectMap<List<VillagerTrades.ItemListing>> WANDERER_TRADES_GENERIC = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<List<VillagerTrades.ItemListing>> WANDERER_TRADES_RARE = new Int2ObjectOpenHashMap<>();
    private static final Map<ItemLike, Integer> FUEL_TIMES = new ConcurrentHashMap<>();

    private static void putFuelTime(int time, ItemLike item) {
        if (time >= 0) {
            FUEL_TIMES.put(item, time);
        } else {
            FUEL_TIMES.remove(item);
        }
    }

    public void perform(RegistrationAction action) {
        if (action instanceof RegistryBatchAction<?> batchRaw) {
            performRegistryBatch((RegistryBatchAction<Object>) batchRaw);
            return;
        }

        if (action instanceof CreativeTabAppendStackAction append) {
            TAB_APPENDS.computeIfAbsent(append.tab().location(), k -> new CopyOnWriteArrayList<>()).add(append.item());
            return;
        }

        if (action instanceof CreativeTabModifyAction modify) {
            TAB_MODIFIERS.computeIfAbsent(modify.tab().location(), k -> new CopyOnWriteArrayList<>()).add(modify.callback());
            return;
        }

        if (action instanceof CommandRegisterAction cra) {
            if (!cra.clientOnly()) {
                SERVER_COMMANDS.add(cra);
            }
            return;
        }

        if (action instanceof EntityAttributeAction attr) {
            ATTRIBUTES.put(attr.type(), attr.attributes());
            return;
        }

        if (action instanceof SpawnPlacementAction<?> spa) {
            SPAWN_PLACEMENTS.add(spa);
            return;
        }

        if (action instanceof TradeVillagerAction villagerTrade) {
            var map = VILLAGER_TRADES.computeIfAbsent(villagerTrade.profession(), k -> new Int2ObjectOpenHashMap<>());
            var list = map.computeIfAbsent(villagerTrade.level(), k -> new ArrayList<>());
            Collections.addAll(list, villagerTrade.trades());
            return;
        }

        if (action instanceof TradeWandererAction wandererTrade) {
            boolean rare = wandererTrade.rare();
            var target = rare ? WANDERER_TRADES_RARE : WANDERER_TRADES_GENERIC;
            var list = target.computeIfAbsent(rare ? 2 : 1, k -> new ArrayList<>());
            Collections.addAll(list, wandererTrade.trades());
            return;
        }

        if (action instanceof FuelAction fuel) {
            int time = fuel.time();
            Supplier<? extends ItemLike>[] items = fuel.items();
            for (var s : items) {
                if (s instanceof RegisterSupplier<?> rs) {
                    RegisterSupplier<? extends ItemLike> itemSupplier = (RegisterSupplier<? extends ItemLike>) rs;
                    itemSupplier.listen(item -> putFuelTime(time, item));
                    continue;
                }
                putFuelTime(time, s.get());
            }
        }
    }

    private <T> void performRegistryBatch(RegistryBatchAction<T> batch) {
        var key = batch.registryKey();
        var modid = batch.modid();
        var dr = DeferredRegister.create(key, modid);
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        dr.register(bus);
        for (RegisterSupplier<? extends T> entry : batch.entries()) {
            Supplier<T> supplier = (Supplier<T>) entry.getCreator();
            var ro = dr.register(entry.id().getPath(), supplier);
            ((RegisterSupplier<T>) entry).setGetter(ro);
        }
        
        bus.addListener(EventPriority.LOWEST, (RegisterEvent event) -> {
            if (event.getRegistryKey().equals(key)) {
                for (RegisterSupplier<? extends T> entry : batch.entries()) {
                    Object obj = entry.get();
                    ((RegisterSupplier<Object>) entry).bindInstance(obj);
                }
            }
        });
    }

    @Mod.EventBusSubscriber(modid = OELibRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventHooks {
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
                        if (after.isEmpty()) {
                            event.getEntries().put(stack, v);
                        } else {
                            event.getEntries().putAfter(after, stack, v);
                        }
                    }

                    @Override
                    public void acceptBefore(ItemStack before, ItemStack stack, CreativeModeTab.TabVisibility v) {
                        if (before.isEmpty()) {
                            event.getEntries().put(stack, v);
                        } else {
                            event.getEntries().putBefore(before, stack, v);
                        }
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
        public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
            for (var spaRaw : SPAWN_PLACEMENTS) {
                var spa = (SpawnPlacementAction<Mob>) spaRaw;
                event.register(spa.type().get(), spa.placement(), spa.heightmap(), spa.predicate(),
                        SpawnPlacementRegisterEvent.Operation.OR);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = OELibRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEventHooks {
        @SubscribeEvent
        public static void onVillagerTrades(VillagerTradesEvent event) {
            var map = VILLAGER_TRADES.get(event.getType());
            if (map != null) {
                for (var entry : map.int2ObjectEntrySet()) {
                    event.getTrades().computeIfAbsent(entry.getIntKey(), k -> NonNullList.create())
                            .addAll(entry.getValue());
                }
            }
        }

        @SubscribeEvent
        public static void onWandererTrades(WandererTradesEvent event) {
            var generic = WANDERER_TRADES_GENERIC.get(1);
            var rare = WANDERER_TRADES_RARE.get(2);
            if (generic != null) event.getGenericTrades().addAll(generic);
            if (rare != null) event.getRareTrades().addAll(rare);
        }

        @SubscribeEvent
        public static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event) {
            var stack = event.getItemStack();
            if (stack.isEmpty()) return;
            int time = FUEL_TIMES.getOrDefault(stack.getItem(), Integer.MIN_VALUE);
            if (time != Integer.MIN_VALUE) event.setBurnTime(time);
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            for (var cra : SERVER_COMMANDS) {
                cra.registrar().register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
            }
        }
    }
}
