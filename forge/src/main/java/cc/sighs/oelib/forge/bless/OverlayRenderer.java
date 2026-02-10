package cc.sighs.oelib.forge.bless;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.bless.render.AbstractShaderOverlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cc.sighs.oelib.bless.OverlayRegistry.REGISTERED_OVERLAYS;

@Mod.EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OverlayRenderer {

    private OverlayRenderer() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        double mouseX = minecraft.mouseHandler.xpos() * (double) width / (double) event.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * (double) height / (double) event.getWindow().getScreenHeight();

        var guiGraphics = event.getGuiGraphics();
        var pose = guiGraphics.pose();

        pose.pushPose();
        pose.translate(0.0F, 0.0F, 1000.0F);

        float partialTick = event.getPartialTick();

        for (AbstractShaderOverlay overlay : REGISTERED_OVERLAYS) {
            if (overlay.isActive()) {
                overlay.render(guiGraphics, partialTick, (int) mouseX, (int) mouseY, width, height);
            }
        }

        pose.popPose();
    }
}