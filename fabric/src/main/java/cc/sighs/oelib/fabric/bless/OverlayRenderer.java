package cc.sighs.oelib.fabric.bless;

import cc.sighs.oelib.bless.render.AbstractShaderOverlay;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import static cc.sighs.oelib.bless.OverlayRegistry.REGISTERED_OVERLAYS;

public class OverlayRenderer {
    public static void register() {
        HudRenderCallback.EVENT.register(OverlayRenderer::onHudRender);
    }

    private static void onHudRender(GuiGraphics guiGraphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        double mouseX = minecraft.mouseHandler.xpos() * (double) width / (double) minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * (double) height / (double) minecraft.getWindow().getScreenHeight();

        for (AbstractShaderOverlay overlay : REGISTERED_OVERLAYS) {
            if (overlay.isActive()) {
                overlay.render(guiGraphics, partialTick, (int) mouseX, (int) mouseY, width, height);
            }
        }
    }
}