package cc.sighs.oelib.bless.neoforge;

import cc.sighs.oelib.bless.OELibBless;
import cc.sighs.oelib.bless.render.AbstractShaderOverlay;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import static cc.sighs.oelib.bless.OverlayRegistry.REGISTERED_OVERLAYS;

@EventBusSubscriber(modid = OELibBless.MOD_ID, value = Dist.CLIENT)
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

        for (AbstractShaderOverlay overlay : REGISTERED_OVERLAYS) {
            if (overlay.isActive()) {
                overlay.render(guiGraphics, partialTick, (int) mouseX, (int) mouseY, width, height);
            }
        }

        pose.popPose();
    }
}
