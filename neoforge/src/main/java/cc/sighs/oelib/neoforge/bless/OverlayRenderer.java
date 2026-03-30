package cc.sighs.oelib.neoforge.bless;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.bless.render.OverlayRenderDispatcher;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class OverlayRenderer {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(OELib.MODID, "bless_toast");

    private OverlayRenderer() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(OverlayRenderer::onRegisterGuiLayers);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.SUBTITLE_OVERLAY, LAYER_ID, OverlayRenderer::onRenderLayer);
    }

    private static void onRenderLayer(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!OverlayRenderDispatcher.shouldRender(minecraft)) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        OverlayRenderDispatcher.renderExtractPass(guiGraphics, width, height);
    }
}
