package cc.sighs.oelib.fabric.bless;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.bless.render.OverlayRenderDispatcher;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class OverlayRenderer {
    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath(OELib.MODID, "bless_toast"),
                OverlayRenderer::onHudRender
        );
    }

    private static void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!OverlayRenderDispatcher.shouldRender(minecraft)) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        OverlayRenderDispatcher.renderExtractPass(guiGraphics, width, height);
    }
}
