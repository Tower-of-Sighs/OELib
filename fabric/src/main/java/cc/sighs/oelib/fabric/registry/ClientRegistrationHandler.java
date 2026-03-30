package cc.sighs.oelib.fabric.registry;

import cc.sighs.oelib.registry.action.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public final class ClientRegistrationHandler {

    private static final Map<Class<?>, Function<?, ? extends ClientTooltipComponent>> TOOLTIP_FACTORIES = new ConcurrentHashMap<>();
    private static final List<CommandRegisterAction> CLIENT_COMMANDS = new CopyOnWriteArrayList<>();
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
            KeyMappingHelper.registerKeyMapping(mapping);
            return;
        }

        if (action instanceof ClientTooltipComponentAction<?> tooltipRaw) {
            performClientTooltip((ClientTooltipComponentAction<TooltipComponent>) tooltipRaw);
            return;
        }

        if (action instanceof ColorItemAction(Identifier identifier, MapCodec<? extends ItemTintSource> codec)) {
            ItemTintSources.ID_MAPPER.put(identifier, codec);
            return;
        }

        if (action instanceof ColorBlockAction(List<BlockTintSource> sources, var blocks)) {
            var arr = Arrays.stream(blocks)
                    .map(Supplier::get)
                    .toArray(Block[]::new);
            BlockColorRegistry.register(sources, arr);
            return;
        }

        if (action instanceof EntityRendererAction<?> eraRaw) {
            var era = (EntityRendererAction<Entity>) eraRaw;
            EntityRenderers.register(era.type().get(), era.provider());
            return;
        }

        if (action instanceof EntityModelLayerAction(
                ModelLayerLocation location, Supplier<LayerDefinition> definition
        )) {
            ModelLayerRegistry.registerModelLayer(location, definition::get);
            return;
        }

        if (action instanceof MenuAction<?, ?> menuAction) {
            registerMenuScreen(menuAction);
            return;
        }

        if (action instanceof ParticleProviderAction<?> ppaRaw) {
            var ppa = (ParticleProviderAction<ParticleOptions>) ppaRaw;
            ParticleProviderRegistry.getInstance().register(ppa.type(), ppa.provider());
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
                    ClientTooltipComponentCallback.EVENT.register(component -> {
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
}