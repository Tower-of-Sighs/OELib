package com.mafuyu404.oelib.fabric.registry;

import com.mafuyu404.oelib.api.registry.IRegistrationDispatcher;
import com.mafuyu404.oelib.registry.RegisterSupplier;
import com.mafuyu404.oelib.registry.action.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RegistrationDispatcherImpl implements IRegistrationDispatcher {
    private static final Map<Class<?>, Function<?, ? extends ClientTooltipComponent>> TOOLTIP_FACTORIES = new ConcurrentHashMap<>();
    private static volatile boolean tooltipCallbackRegistered = false;

    @SuppressWarnings("unchecked")
    @Override
    public void perform(RegistrationAction action) {
        if (action instanceof RegistryBatchAction<?> batchRaw) {
            performRegistryBatch((RegistryBatchAction<Object>) batchRaw);
            return;
        }
        if (action instanceof KeyMappingAction key) {
            performKeyMapping(key);
            return;
        }
        if (action instanceof ClientTooltipComponentAction<?> tooltipRaw) {
            performClientTooltip((ClientTooltipComponentAction<TooltipComponent>) tooltipRaw);
            return;
        }

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            if (action instanceof ColorItemAction(
                    ItemColor color, Supplier<? extends ItemLike>[] items
            )) {
                var arr = new ItemLike[items.length];
                for (int i = 0; i < items.length; i++) arr[i] = Objects.requireNonNull(items[i].get());
                ColorProviderRegistry.ITEM.register(color, arr);
                return;
            }
            if (action instanceof ColorBlockAction(
                    BlockColor color,
                    Supplier<? extends Block>[] blocks
            )) {
                var arr = new Block[blocks.length];
                for (int i = 0; i < blocks.length; i++) arr[i] = Objects.requireNonNull(blocks[i].get());
                ColorProviderRegistry.BLOCK.register(color, arr);
                return;
            }
            if (action instanceof RenderTypeBlocksAction(
                    RenderType type, Block[] blocks
            )) {
                BlockRenderLayerMap.INSTANCE.putBlocks(type, blocks);
                return;
            }
            if (action instanceof RenderTypeFluidsAction(
                    RenderType type, Fluid[] fluids
            )) {
                BlockRenderLayerMap.INSTANCE.putFluids(type, fluids);
                return;
            }
            if (action instanceof EntityRendererAction<?> eraRaw) {
                var era = (EntityRendererAction<Entity>) eraRaw;
                EntityRendererRegistry.register(era.type().get(), era.provider());
                return;
            }
            if (action instanceof EntityModelLayerAction(
                    ModelLayerLocation location,
                    Supplier<LayerDefinition> definition
            )) {
                EntityModelLayerRegistry.registerModelLayer(location, definition::get);
                return;
            }
            if (action instanceof ParticleProviderAction<?> ppaRaw) {
                var ppa = (ParticleProviderAction<ParticleOptions>) ppaRaw;
                ParticleFactoryRegistry.getInstance().register(
                        ppa.type(),
                        ppa.provider()
                );
                return;
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
                    VillagerProfession profession, int level,
                    VillagerTrades.ItemListing[] trades
            )) {
                TradeOfferHelper.registerVillagerOffers(profession, level,
                        list -> Collections.addAll(list, trades));
                return;
            }
            if (action instanceof TradeWandererAction(
                    boolean rare, VillagerTrades.ItemListing[] trades
            )) {
                TradeOfferHelper.registerWanderingTraderOffers(rare ? 2 : 1,
                        list -> Collections.addAll(list, trades));
                return;
            }
            if (action instanceof FuelAction(int time, ItemLike[] items)) {
                for (var item : items) {
                    if (time >= 0) {
                        FuelRegistry.INSTANCE.add(item, time);
                    } else {
                        FuelRegistry.INSTANCE.remove(item);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void performRegistryBatch(RegistryBatchAction<T> batch) {
        var rawRegistry = BuiltInRegistries.REGISTRY.get(batch.registryKey().location());
        if (rawRegistry == null) {
            throw new IllegalStateException("Registry not found for key: " + batch.registryKey().location());
        }
        var registry = (Registry<T>) rawRegistry;

        for (RegisterSupplier<? extends T> entry : batch.entries()) {
            T instance = entry.getCreator().get();
            Registry.register(registry, entry.id(), instance);
            ((RegisterSupplier<T>) entry).bindInstance(instance);
        }
    }

    private void performKeyMapping(KeyMappingAction key) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            KeyBindingHelper.registerKeyBinding(key.mapping());
        }
    }

    private void performClientTooltip(ClientTooltipComponentAction<TooltipComponent> tooltip) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }
        ensureTooltipCallbackRegistered();
        TOOLTIP_FACTORIES.put(tooltip.clazz(), tooltip.factory());
    }

    @SuppressWarnings("unchecked")
    private void ensureTooltipCallbackRegistered() {
        if (!tooltipCallbackRegistered) {
            synchronized (TOOLTIP_FACTORIES) {
                if (!tooltipCallbackRegistered) {
                    TooltipComponentCallback.EVENT.register(component -> {
                        var fn = (Function<TooltipComponent, ClientTooltipComponent>)
                                TOOLTIP_FACTORIES.get(component.getClass());
                        return fn != null ? fn.apply(component) : null;
                    });
                    tooltipCallbackRegistered = true;
                }
            }
        }
    }
}