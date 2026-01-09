package cc.sighs.oelib.fabric.registry;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.registry.action.*;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public final class ClientRegistrationHandler {

    private static final Map<Class<?>, Function<?, ? extends ClientTooltipComponent>> TOOLTIP_FACTORIES = new ConcurrentHashMap<>();
    private static final List<CommandRegisterAction> CLIENT_COMMANDS = new CopyOnWriteArrayList<>();
    private static final List<ShaderRegisterAction> SHADER_REGISTRATIONS = new CopyOnWriteArrayList<>();
    private static volatile boolean tooltipCallbackRegistered = false;
    private static volatile boolean clientCommandEventsInstalled = false;
    private static volatile boolean shaderEventsInstalled = false;

    public void perform(RegistrationAction action) {
        if (action instanceof CommandRegisterAction cra) {
            if (cra.clientOnly()) {
                ensureClientCommandEvents();
                CLIENT_COMMANDS.add(cra);
            }
            return;
        }

        if (action instanceof KeyMappingAction(KeyMapping mapping)) {
            KeyBindingHelper.registerKeyBinding(mapping);
            return;
        }

        if (action instanceof ClientTooltipComponentAction<?> tooltipRaw) {
            performClientTooltip((ClientTooltipComponentAction<TooltipComponent>) tooltipRaw);
            return;
        }

        if (action instanceof ShaderRegisterAction shader) {
            ensureShaderEventRegistered();
            SHADER_REGISTRATIONS.add(shader);
            return;
        }

        if (action instanceof ColorItemAction(ItemColor color, Supplier<? extends ItemLike>[] items)) {
            var arr = new ItemLike[items.length];
            for (int i = 0; i < items.length; i++) arr[i] = Objects.requireNonNull(items[i].get());
            ColorProviderRegistry.ITEM.register(color, arr);
            return;
        }

        if (action instanceof ColorBlockAction(BlockColor color, Supplier<? extends Block>[] blocks)) {
            var arr = new Block[blocks.length];
            for (int i = 0; i < blocks.length; i++) arr[i] = Objects.requireNonNull(blocks[i].get());
            ColorProviderRegistry.BLOCK.register(color, arr);
            return;
        }

        if (action instanceof RenderTypeBlocksAction(RenderType type, Supplier<? extends Block>[] blocks)) {
            var arr = new Block[blocks.length];
            for (int i = 0; i < blocks.length; i++) arr[i] = Objects.requireNonNull(blocks[i].get());
            BlockRenderLayerMap.INSTANCE.putBlocks(type, arr);
            return;
        }

        if (action instanceof RenderTypeFluidsAction(RenderType type, Supplier<? extends Fluid>[] fluids)) {
            var arr = new Fluid[fluids.length];
            for (int i = 0; i < fluids.length; i++) arr[i] = Objects.requireNonNull(fluids[i].get());
            BlockRenderLayerMap.INSTANCE.putFluids(type, arr);
            return;
        }

        if (action instanceof EntityRendererAction<?> eraRaw) {
            var era = (EntityRendererAction<Entity>) eraRaw;
            EntityRendererRegistry.register(era.type().get(), era.provider());
            return;
        }

        if (action instanceof EntityModelLayerAction(
                ModelLayerLocation location, Supplier<LayerDefinition> definition
        )) {
            EntityModelLayerRegistry.registerModelLayer(location, definition::get);
            return;
        }

        if (action instanceof MenuAction<?, ?> menuAction) {
            registerMenuScreen(menuAction);
            return;
        }

        if (action instanceof ParticleProviderAction<?> ppaRaw) {
            var ppa = (ParticleProviderAction<ParticleOptions>) ppaRaw;
            ParticleFactoryRegistry.getInstance().register(ppa.type(), ppa.provider());
        }
    }

    private void performClientTooltip(ClientTooltipComponentAction<TooltipComponent> tooltip) {
        ensureTooltipCallbackRegistered();
        TOOLTIP_FACTORIES.put(tooltip.clazz(), tooltip.factory());
    }

    private void ensureTooltipCallbackRegistered() {
        if (!tooltipCallbackRegistered) {
            synchronized (TOOLTIP_FACTORIES) {
                if (!tooltipCallbackRegistered) {
                    TooltipComponentCallback.EVENT.register(component -> {
                        var fn = (Function<TooltipComponent, ClientTooltipComponent>) TOOLTIP_FACTORIES.get(component.getClass());
                        return fn != null ? fn.apply(component) : null;
                    });
                    tooltipCallbackRegistered = true;
                }
            }
        }
    }

    private void ensureClientCommandEvents() {
        if (!clientCommandEventsInstalled) {
            synchronized (CLIENT_COMMANDS) {
                if (!clientCommandEventsInstalled) {
                    ClientCommandRegistrationCallback.EVENT.register(this::handleClientCommandRegistration);
                    clientCommandEventsInstalled = true;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleClientCommandRegistration(Object dispatcher, CommandBuildContext context) {
        CommandDispatcher<CommandSourceStack> casted = (CommandDispatcher<CommandSourceStack>) dispatcher;
        Commands.CommandSelection environment = Commands.CommandSelection.INTEGRATED;
        for (var cra : CLIENT_COMMANDS) {
            cra.registrar().register(casted, context, environment);
        }
    }

    private <H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> void registerMenuScreen(MenuAction<H, S> action) {
        MenuScreens.register(action.type().get(), action.factory()::create);
    }

    private void ensureShaderEventRegistered() {
        if (!shaderEventsInstalled) {
            synchronized (SHADER_REGISTRATIONS) {
                if (!shaderEventsInstalled) {
                    CoreShaderRegistrationCallback.EVENT.register(context -> {
                        for (var shader : SHADER_REGISTRATIONS) {
                            try {
                                context.register(shader.id(), shader.vertexFormat(), shader.loadCallback());
                            } catch (IOException e) {
                                OELib.LOGGER.error("Failed to register shader {}", shader.id(), e);
                            }
                        }
                    });
                    shaderEventsInstalled = true;
                }
            }
        }
    }
}