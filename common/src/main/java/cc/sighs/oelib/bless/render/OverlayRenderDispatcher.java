package cc.sighs.oelib.bless.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static cc.sighs.oelib.bless.OverlayRegistry.REGISTERED_OVERLAYS;

public final class OverlayRenderDispatcher {
    private OverlayRenderDispatcher() {
    }

    public static boolean shouldRender(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && !minecraft.options.hideGui;
    }

    public static void renderExtractPass(GuiGraphicsExtractor guiGraphics, int width, int height) {
        for (AbstractShaderOverlay overlay : REGISTERED_OVERLAYS) {
            if (overlay.isActive()) {
                overlay.renderExtract(guiGraphics, width, height);
            }
        }
    }

    public static void renderBackgroundPass(Minecraft minecraft, int width, int height) {
        var window = minecraft.getWindow();
        int screenWidth = Math.max(1, window.getScreenWidth());
        int screenHeight = Math.max(1, window.getScreenHeight());

        int mouseX = (int) (minecraft.mouseHandler.xpos() * (double) width / (double) screenWidth);
        int mouseY = (int) (minecraft.mouseHandler.ypos() * (double) height / (double) screenHeight);

        for (AbstractShaderOverlay overlay : REGISTERED_OVERLAYS) {
            if (overlay.isActive()) {
                overlay.renderBackground(mouseX, mouseY, width, height);
            }
        }
    }
}
