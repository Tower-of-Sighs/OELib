package cc.sighs.oelib.bless.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public abstract class AbstractShaderOverlay {
    private final long showDuration;
    private final long fadeDuration;
    private final long slideDuration;
    private long startTime;
    private boolean active;

    protected AbstractShaderOverlay(long showDuration, long fadeDuration, long slideDuration) {
        this.showDuration = showDuration;
        this.fadeDuration = fadeDuration;
        this.slideDuration = slideDuration;
    }

    public boolean isActive() {
        return active;
    }

    public void show() {
        active = true;
        startTime = Util.getMillis();
        var minecraft = Minecraft.getInstance();
        onShow(minecraft);
    }

    public void render(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, int width, int height) {
        if (!active) {
            return;
        }
        var shader = getShader();
        if (shader == null) {
            return;
        }

        AnimationState animationState = calculateAnimationState(width, height);
        if (!animationState.shouldRender()) {
            active = false;
            var minecraft = Minecraft.getInstance();
            onHide(minecraft);
            return;
        }
        renderShaderBackground(guiGraphics, shader, animationState, mouseX, mouseY, width, height);

        renderTextContent(guiGraphics, animationState);
    }

    private AnimationState calculateAnimationState(int screenWidth, int screenHeight) {
        long now = Util.getMillis();
        long visibleTime = Math.max(now - startTime, 0L);
        boolean leaving = visibleTime >= showDuration;
        long fadeTime = leaving ? visibleTime - showDuration : visibleTime;
        float fade = Mth.clamp((float) fadeTime / (float) fadeDuration, 0.0F, 1.0F);
        float alpha = leaving ? 1.0F - fade : fade;

        if (leaving && alpha <= 0.0F) {
            return new AnimationState(false, 0, 0, 0, 0, 0, 0);
        }

        int overlayWidth = overlayWidth();
        int overlayHeight = overlayHeight();
        float baseX = baseX(screenWidth, overlayWidth);
        float baseY = baseY(screenHeight, overlayHeight);

        float slideT = calculateSlideT(visibleTime);
        float slideOffset = (1.0F - slideT) * (float) overlayWidth;
        float x = baseX + slideOffset;
        float y = baseY;

        return new AnimationState(true, x, y, alpha, overlayWidth, overlayHeight, visibleTime);
    }

    private float calculateSlideT(long visibleTime) {
        if (visibleTime <= slideDuration) {
            return (float) visibleTime / (float) slideDuration;
        } else if (visibleTime >= showDuration) {
            long leaveElapsed = Math.min(visibleTime - showDuration, slideDuration);
            return 1.0F - (float) leaveElapsed / (float) slideDuration;
        } else {
            return 1.0F;
        }
    }

    private void renderShaderBackground(GuiGraphics guiGraphics, ShaderInstance shader,
                                        AnimationState state, int mouseX, int mouseY,
                                        int screenWidth, int screenHeight) {
        float timeSeconds = (float) (Util.getMillis() % 100000L) / 1000.0F;

        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(this::getShader);

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(state.x, state.y, 0.0F);
        var currentPose = pose.last().pose();

        applyUniforms(shader, timeSeconds, state.alpha, state.overlayWidth,
                state.overlayHeight, mouseX, mouseY, screenWidth, screenHeight);

        var tesselator = Tesselator.getInstance();
        var bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(currentPose, 0.0F, 0.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        bufferBuilder.vertex(currentPose, 0.0F, (float) state.overlayHeight, 0.0F).uv(0.0F, 1.0F).endVertex();
        bufferBuilder.vertex(currentPose, (float) state.overlayWidth, (float) state.overlayHeight, 0.0F).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(currentPose, (float) state.overlayWidth, 0.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        pose.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderTextContent(GuiGraphics guiGraphics, AnimationState state) {
        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        var lines = textLines(minecraft);

        if (lines.length > 0) {
            int color = textColor();
            float centerX = state.x + (float) state.overlayWidth / 2.0F;
            float innerWidth = (float) state.overlayWidth - horizontalPadding() * 2.0F;
            if (innerWidth < 0.0F) {
                innerWidth = 0.0F;
            }

            float titleScale = titleScale();
            float bodyScale = bodyScale();
            float currentY = state.y + 8.0F;

            var title = lines[0];
            drawScaledCenteredString(guiGraphics, font, title, centerX, currentY,
                    color, (int) innerWidth, titleScale);
            currentY += (float) font.lineHeight * titleScale + 2.0F;

            if (hasSeparator() && lines.length > 1) {
                float factor = Mth.clamp(separatorLengthFactor(), 0.0F, 1.0F);
                float length = innerWidth * factor;
                float half = length / 2.0F;
                int lineY = Mth.floor(currentY + (float) font.lineHeight * bodyScale / 2.0F);
                int startX = Mth.floor(centerX - half);
                int endX = Mth.floor(centerX + half);
                guiGraphics.fill(startX, lineY, endX, lineY + 1, color);
                currentY += (float) font.lineHeight * bodyScale + 2.0F;
            }

            for (int i = 1; i < lines.length; i++) {
                drawScaledCenteredString(guiGraphics, font, lines[i], centerX, currentY,
                        color, (int) innerWidth, bodyScale);
                currentY += (float) font.lineHeight * bodyScale + 2.0F;
            }
        }
    }

    protected float baseX(int screenWidth, int overlayWidth) {
        return (float) screenWidth - (float) overlayWidth - 10.0F;
    }

    protected float baseY(int screenHeight, int overlayHeight) {
        return 10.0F;
    }

    protected void onShow(Minecraft minecraft) {
    }

    protected void onHide(Minecraft minecraft) {
    }

    protected Component[] textLines(Minecraft minecraft) {
        return new Component[0];
    }

    protected int textColor() {
        return 0xFFF8E6B5;
    }

    protected float horizontalPadding() {
        return 16.0F;
    }

    protected boolean hasSeparator() {
        return false;
    }

    protected float separatorLengthFactor() {
        return 0.6F;
    }

    protected float titleScale() {
        return 1.0F;
    }

    protected float bodyScale() {
        return 0.78F;
    }

    public boolean isChineseFestival() {
        return false;
    }

    public abstract String festivalId();

    public abstract String festivalName();

    private void drawScaledCenteredString(GuiGraphics guiGraphics, Font font, Component text, float centerX, float y, int color, int maxWidth, float baseScale) {
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(centerX, y, 0.0F);
        pose.scale(baseScale, baseScale, 1.0F);
        guiGraphics.drawCenteredString(font, text, 0, 0, color);
        pose.popPose();
    }

    protected abstract ShaderInstance getShader();

    protected abstract int overlayWidth();

    protected abstract int overlayHeight();

    protected abstract void applyUniforms(ShaderInstance shader, float timeSeconds, float alpha, int overlayWidth, int overlayHeight, int mouseX, int mouseY, int screenWidth, int screenHeight);

    private record AnimationState(
            boolean shouldRender,
            float x,
            float y,
            float alpha,
            int overlayWidth,
            int overlayHeight,
            long visibleTime
    ) {}
}
