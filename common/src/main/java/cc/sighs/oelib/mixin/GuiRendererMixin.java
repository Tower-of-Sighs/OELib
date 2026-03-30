package cc.sighs.oelib.mixin;

import cc.sighs.oelib.bless.render.OverlayRenderDispatcher;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(
            method = "draw(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void oelib$renderOverlayShaderAfterGuiProjection(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        if (!OverlayRenderDispatcher.shouldRender(minecraft)) {
            return;
        }

        var window = minecraft.getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        OverlayRenderDispatcher.renderBackgroundPass(minecraft, width, height);
    }
}
