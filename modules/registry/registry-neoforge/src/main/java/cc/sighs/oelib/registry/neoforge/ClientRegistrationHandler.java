package cc.sighs.oelib.registry.neoforge;

import cc.sighs.oelib.registry.OELibRegistry;
import cc.sighs.oelib.registry.action.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@SuppressWarnings({"unused", "unchecked"})
public final class ClientRegistrationHandler {

    private static final Queue<KeyMapping> KEY_MAPPINGS = new ConcurrentLinkedQueue<>();
    private static final Map<Class<?>, Function<?, ? extends ClientTooltipComponent>> TOOLTIP_FACTORIES = new ConcurrentHashMap<>();
    private static final List<ColorItemAction> ITEM_COLORS = new CopyOnWriteArrayList<>();
    private static final List<ColorBlockAction> BLOCK_COLORS = new CopyOnWriteArrayList<>();
    private static final List<EntityRendererAction<?>> ENTITY_RENDERERS = new CopyOnWriteArrayList<>();
    private static final List<EntityModelLayerAction> MODEL_LAYERS = new CopyOnWriteArrayList<>();
    private static final List<ParticleProviderAction<?>> PARTICLE_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<RenderTypeBlocksAction> RENDER_BLOCKS = new CopyOnWriteArrayList<>();
    private static final List<RenderTypeFluidsAction> RENDER_FLUIDS = new CopyOnWriteArrayList<>();
    private static final List<CommandRegisterAction> CLIENT_COMMANDS = new CopyOnWriteArrayList<>();
    private static final List<ShaderRegisterAction> SHADERS = new CopyOnWriteArrayList<>();
    private static final List<MenuAction<?, ?>> MENU_SCREENS = new CopyOnWriteArrayList<>();

    public void perform(RegistrationAction action) {
        if (action instanceof KeyMappingAction(KeyMapping mapping)) {
            KEY_MAPPINGS.add(mapping);
            return;
        }

        if (action instanceof ShaderRegisterAction shader) {
            SHADERS.add(shader);
            return;
        }

        if (action instanceof ClientTooltipComponentAction<?> tooltipRaw) {
            var tooltip = (ClientTooltipComponentAction<TooltipComponent>) tooltipRaw;
            TOOLTIP_FACTORIES.put(tooltip.clazz(), tooltip.factory());
            return;
        }

        if (action instanceof ColorItemAction cia) {
            ITEM_COLORS.add(cia);
            return;
        }

        if (action instanceof ColorBlockAction cba) {
            BLOCK_COLORS.add(cba);
            return;
        }

        if (action instanceof RenderTypeBlocksAction rtba) {
            RENDER_BLOCKS.add(rtba);
            return;
        }

        if (action instanceof RenderTypeFluidsAction rtfa) {
            RENDER_FLUIDS.add(rtfa);
            return;
        }

        if (action instanceof EntityRendererAction<?> era) {
            ENTITY_RENDERERS.add(era);
            return;
        }

        if (action instanceof EntityModelLayerAction emla) {
            MODEL_LAYERS.add(emla);
            return;
        }

        if (action instanceof MenuAction<?, ?> ma) {
            MENU_SCREENS.add(ma);
            return;
        }

        if (action instanceof ParticleProviderAction<?> ppa) {
            PARTICLE_PROVIDERS.add(ppa);
            return;
        }

        if (action instanceof CommandRegisterAction cra && cra.clientOnly()) {
            CLIENT_COMMANDS.add(cra);
        }
    }

    @SuppressWarnings("deprecation")
    @EventBusSubscriber(modid = OELibRegistry.MOD_ID, value = Dist.CLIENT)
    public static class ClientEventHooks {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            KeyMapping km;
            while ((km = KEY_MAPPINGS.poll()) != null) {
                event.register(km);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
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
                event.registerSpecial(ppa.type(), ppa.provider());
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
        public static void onRegisterShaders(RegisterShadersEvent event) {
            for (var shader : SHADERS) {
                try {
                    ShaderInstance instance = new ShaderInstance(
                            event.getResourceProvider(),
                            shader.id(),
                            shader.vertexFormat()
                    );
                    event.registerShader(instance, shader.loadCallback());
                } catch (Exception e) {
                    OELibRegistry.LOGGER.error("Failed to register shader {}", shader.id(), e);
                }
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

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            for (var cra : CLIENT_COMMANDS) {
                cra.registrar().register(
                        event.getDispatcher(),
                        event.getBuildContext(),
                        Commands.CommandSelection.INTEGRATED
                );
            }
        }

        @SubscribeEvent
        public static void onRegisterMenus(RegisterMenuScreensEvent event) {
            for (var ma : MENU_SCREENS) {
                registerMenuScreen(event, ma);
            }
        }

        private static <H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> void registerMenuScreen(RegisterMenuScreensEvent event, MenuAction<H, S> action) {
            event.register(action.type().get(), action.factory()::create);
        }
    }
}