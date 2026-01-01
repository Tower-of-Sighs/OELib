package com.sighs.oelib.neoforge.bless;

import com.sighs.oelib.OELib;
import com.sighs.oelib.bless.render.ChongYangOverlay;
import com.sighs.oelib.bless.render.NewYearOverlay;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
public class OverlayRenderer {
    private OverlayRenderer() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        var window = minecraft.getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();

        double mouseX = minecraft.mouseHandler.xpos() * (double) width / (double) window.getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * (double) height / (double) window.getScreenHeight();

        var guiGraphics = event.getGuiGraphics();
        var pose = guiGraphics.pose();

        pose.pushPose();
        pose.translate(0.0F, 0.0F, 500.0F);

        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();

        if (ChongYangOverlay.INSTANCE.isActive()) {
            ChongYangOverlay.INSTANCE.render(guiGraphics, partialTick, (int) mouseX, (int) mouseY, width, height);
        }

        if (NewYearOverlay.INSTANCE.isActive()) {
            NewYearOverlay.INSTANCE.render(guiGraphics, partialTick, (int) mouseX, (int) mouseY, width, height);
        }

        pose.popPose();
    }
}
