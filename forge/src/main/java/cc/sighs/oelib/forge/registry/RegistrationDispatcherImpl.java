package cc.sighs.oelib.forge.registry;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.registry.RegisterSupplier;
import cc.sighs.oelib.registry.action.*;
import cc.sighs.oelib.registry.api.CreativeTabModifyCallback;
import cc.sighs.oelib.registry.api.CreativeTabOutput;
import cc.sighs.oelib.registry.spi.IRegistrationDispatcher;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.util.MutableHashedLinkedMap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RegistrationDispatcherImpl implements IRegistrationDispatcher {

    private static final Queue<KeyMapping> KEY_MAPPINGS = new ConcurrentLinkedQueue<>();
    private static final Map<Class<?>, Function<?, ? extends ClientTooltipComponent>> TOOLTIP_FACTORIES = new ConcurrentHashMap<>();
    private static final List<ColorItemAction> ITEM_COLORS = new CopyOnWriteArrayList<>();
    private static final List<ColorBlockAction> BLOCK_COLORS = new CopyOnWriteArrayList<>();
    private static final List<EntityRendererAction<?>> ENTITY_RENDERERS = new CopyOnWriteArrayList<>();
    private static final List<EntityModelLayerAction> MODEL_LAYERS = new CopyOnWriteArrayList<>();
    private static final List<ParticleProviderAction<?>> PARTICLE_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<RenderTypeBlocksAction> RENDER_BLOCKS = new CopyOnWriteArrayList<>();
    private static final List<RenderTypeFluidsAction> RENDER_FLUIDS = new CopyOnWriteArrayList<>();
    private static final Map<Supplier<? extends EntityType<? extends LivingEntity>>, Supplier<AttributeSupplier.Builder>> ATTRIBUTES = new ConcurrentHashMap<>();
    private static final List<SpawnPlacementAction<?>> SPAWN_PLACEMENTS = new CopyOnWriteArrayList<>();
    private static final Int2ObjectMap<List<VillagerTrades.ItemListing>> WANDERER_TRADES_GENERIC = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<List<VillagerTrades.ItemListing>> WANDERER_TRADES_RARE = new Int2ObjectOpenHashMap<>();
    private static final Map<VillagerProfession, Int2ObjectMap<List<VillagerTrades.ItemListing>>> VILLAGER_TRADES = new ConcurrentHashMap<>();
    private static final Map<ItemLike, Integer> FUEL_TIMES = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<?>, List<ListenEntry<?>>> ENTRY_LISTENERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<Supplier<ItemStack>>> TAB_APPENDS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<CreativeTabModifyCallback>> TAB_MODIFIERS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<?>, List<RegisterSupplier<?>>> REGISTERED_ENTRIES = new ConcurrentHashMap<>();

    private static void putFuelTime(int time, ItemLike item) {
        if (time >= 0) {
            FUEL_TIMES.put(item, time);
        } else {
            FUEL_TIMES.remove(item);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void perform(RegistrationAction action) {
        if (action instanceof RegistryBatchAction<?>) {
            RegistryBatchAction<Object> batchRaw = (RegistryBatchAction<Object>) action;
            performRegistryBatch(batchRaw);
            return;
        }

        if (action instanceof ListenAction<?>) {
            ListenAction<Object> la = (ListenAction<Object>) action;
            ENTRY_LISTENERS.computeIfAbsent(la.registryKey(), k -> new CopyOnWriteArrayList<>())
                    .add(new ListenEntry<>(la.supplier(), la.callback()));
            return;
        }

        if (action instanceof CreativeTabAppendStackAction appendAction) {
            ResourceKey<CreativeModeTab> tab = appendAction.tab();
            Supplier<ItemStack> item = appendAction.item();
            TAB_APPENDS.computeIfAbsent(tab.location(), k -> new CopyOnWriteArrayList<>()).add(item);
            return;
        }

        if (action instanceof CreativeTabModifyAction modifyAction) {
            ResourceKey<CreativeModeTab> tab = modifyAction.tab();
            CreativeTabModifyCallback callback = modifyAction.callback();
            TAB_MODIFIERS.computeIfAbsent(tab.location(), k -> new CopyOnWriteArrayList<>()).add(callback);
            return;
        }

        if (FMLEnvironment.dist == Dist.CLIENT) {
            if (action instanceof KeyMappingAction) {
                KeyMapping mapping = ((KeyMappingAction) action).mapping();
                KEY_MAPPINGS.add(mapping);
                return;
            }

            if (action instanceof ClientTooltipComponentAction<?>) {
                ClientTooltipComponentAction<TooltipComponent> tooltip =
                        (ClientTooltipComponentAction<TooltipComponent>) action;
                TOOLTIP_FACTORIES.put(tooltip.clazz(), tooltip.factory());
                return;
            }

            if (action instanceof ColorItemAction) {
                ITEM_COLORS.add((ColorItemAction) action);
                return;
            }

            if (action instanceof ColorBlockAction) {
                BLOCK_COLORS.add((ColorBlockAction) action);
                return;
            }

            if (action instanceof RenderTypeBlocksAction) {
                RENDER_BLOCKS.add((RenderTypeBlocksAction) action);
                return;
            }

            if (action instanceof RenderTypeFluidsAction) {
                RENDER_FLUIDS.add((RenderTypeFluidsAction) action);
                return;
            }

            if (action instanceof EntityRendererAction<?>) {
                ENTITY_RENDERERS.add((EntityRendererAction<?>) action);
                return;
            }

            if (action instanceof EntityModelLayerAction) {
                MODEL_LAYERS.add((EntityModelLayerAction) action);
                return;
            }

            if (action instanceof ParticleProviderAction<?>) {
                PARTICLE_PROVIDERS.add((ParticleProviderAction<?>) action);
                return;
            }
        }

        if (action instanceof EntityAttributeAction attributeAction) {
            Supplier<? extends EntityType<? extends LivingEntity>> type = attributeAction.type();
            Supplier<AttributeSupplier.Builder> attributes = attributeAction.attributes();
            ATTRIBUTES.put(type, attributes);
            return;
        }

        if (action instanceof SpawnPlacementAction<?>) {
            SPAWN_PLACEMENTS.add((SpawnPlacementAction<?>) action);
            return;
        }

        if (action instanceof TradeVillagerAction tradeAction) {
            VillagerProfession profession = tradeAction.profession();
            int level = tradeAction.level();
            VillagerTrades.ItemListing[] trades = tradeAction.trades();

            var map = VILLAGER_TRADES.computeIfAbsent(profession, k -> new Int2ObjectOpenHashMap<>());
            var list = map.computeIfAbsent(level, k -> new ArrayList<>());
            Collections.addAll(list, trades);
            return;
        }

        if (action instanceof TradeWandererAction tradeAction) {
            boolean rare = tradeAction.rare();
            VillagerTrades.ItemListing[] trades = tradeAction.trades();

            var target = rare ? WANDERER_TRADES_RARE : WANDERER_TRADES_GENERIC;
            var list = target.computeIfAbsent(rare ? 2 : 1, k -> new ArrayList<>());
            Collections.addAll(list, trades);
            return;
        }

        if (action instanceof FuelAction fuelAction) {
            int time = fuelAction.time();
            Supplier<? extends ItemLike>[] items = fuelAction.items();
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

    @SuppressWarnings({"unchecked"})
    private <T> void performRegistryBatch(RegistryBatchAction<T> batch) {
        var key = batch.registryKey();
        var modid = batch.modid();
        var dr = DeferredRegister.create(key, modid);
        dr.register(FMLJavaModLoadingContext.get().getModEventBus());
        for (RegisterSupplier<? extends T> entry : batch.entries()) {
            @SuppressWarnings("unchecked")
            Supplier<T> supplier = (Supplier<T>) entry.getCreator();
            var ro = dr.register(entry.id().getPath(), supplier);
            ((RegisterSupplier<T>) entry).setGetter(ro);
        }
        REGISTERED_ENTRIES.computeIfAbsent(key, _k -> new CopyOnWriteArrayList<>()).addAll(batch.entries());
    }

    private record ListenEntry<T>(RegisterSupplier<? extends T> supplier, Consumer<? super T> callback) {
    }

    @SuppressWarnings("unchecked")
    @Mod.EventBusSubscriber(modid = OELib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientEventHooks {
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
                    private final MutableHashedLinkedMap<ItemStack, CreativeModeTab.TabVisibility> entries = event.getEntries();

                    @Override
                    public void acceptAfter(ItemStack after, ItemStack stack, CreativeModeTab.TabVisibility v) {
                        if (after.isEmpty()) {
                            entries.put(stack, v);
                        } else {
                            entries.putAfter(after, stack, v);
                        }
                    }

                    @Override
                    public void acceptBefore(ItemStack before, ItemStack stack, CreativeModeTab.TabVisibility v) {
                        if (before.isEmpty()) {
                            entries.put(stack, v);
                        } else {
                            entries.putBefore(before, stack, v);
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
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            KeyMapping km;
            while ((km = KEY_MAPPINGS.poll()) != null) {
                event.register(km);
            }
        }

        @SubscribeEvent
        public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
            for (var e : TOOLTIP_FACTORIES.entrySet()) {
                var clazz = (Class<? extends TooltipComponent>) e.getKey();
                var fn = (Function<TooltipComponent, ClientTooltipComponent>) e.getValue();
                event.register(clazz, fn);
            }
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            for (var eraRaw : ENTITY_RENDERERS) {
                var era = (EntityRendererAction<Entity>) eraRaw;
                event.registerEntityRenderer(era.type().get(), era.provider());
            }
        }

        @SubscribeEvent
        public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            for (var emla : MODEL_LAYERS) {
                event.registerLayerDefinition(emla.location(), () -> emla.definition().get());
            }
        }

        @SubscribeEvent
        public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
            for (var ppaRaw : PARTICLE_PROVIDERS) {
                var ppa = (ParticleProviderAction<ParticleOptions>) ppaRaw;
                event.registerSpecial(ppa.type(),
                        ppa.provider());
            }
        }

        @SubscribeEvent
        public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
            for (var cia : ITEM_COLORS) {
                var arr = new Item[cia.items().length];
                for (int i = 0; i < cia.items().length; i++) arr[i] = cia.items()[i].get().asItem();
                event.register(cia.color(), arr);
            }
        }

        @SubscribeEvent
        public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
            for (var cba : BLOCK_COLORS) {
                var arr = new Block[cba.blocks().length];
                for (int i = 0; i < cba.blocks().length; i++) arr[i] = cba.blocks()[i].get();
                event.register(cba.color(), arr);
            }
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                for (var rt : RENDER_BLOCKS) {
                    for (var b : rt.blocks()) ItemBlockRenderTypes.setRenderLayer(b.get(), rt.type());
                }
                for (var rf : RENDER_FLUIDS) {
                    for (var f : rf.fluids()) ItemBlockRenderTypes.setRenderLayer(f.get(), rf.type());
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    @Mod.EventBusSubscriber(modid = OELib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEventHooks {
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

    @Mod.EventBusSubscriber(modid = OELib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEventHooks {
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
    }
}