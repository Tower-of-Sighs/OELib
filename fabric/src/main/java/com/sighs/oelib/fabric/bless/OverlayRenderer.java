package com.sighs.oelib.fabric.bless;

import com.sighs.oelib.bless.render.ChongYangOverlay;
import com.sighs.oelib.bless.render.NewYearOverlay;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class OverlayRenderer {
    public static void register() {
        HudRenderCallback.EVENT.register(OverlayRenderer::onHudRender);
    }

    private static void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        double mouseX = minecraft.mouseHandler.xpos() * (double) width / (double) minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * (double) height / (double) minecraft.getWindow().getScreenHeight();

        var pose = guiGraphics.pose();

        pose.pushPose();
        pose.translate(0.0F, 0.0F, 500.0F);

        float partialTick = deltaTracker.getGameTimeDeltaTicks();

        if (ChongYangOverlay.INSTANCE.isActive()) {
            ChongYangOverlay.INSTANCE.render(guiGraphics, partialTick, (int) mouseX, (int) mouseY, width, height);
        }

        if (NewYearOverlay.INSTANCE.isActive()) {
            NewYearOverlay.INSTANCE.render(guiGraphics, partialTick, (int) mouseX, (int) mouseY, width, height);
        }

        pose.popPose();
    }
}