package cc.sighs.oelib.bless.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

public abstract class AbstractShaderOverlay {
    private static final Vector4f WHITE_COLOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f ZERO_OFFSET = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Matrix4f IDENTITY_TEXTURE_MATRIX = new Matrix4f();

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
        onShow(Minecraft.getInstance());
    }

    public final void renderExtract(GuiGraphicsExtractor guiGraphics, int width, int height) {
        AnimationState state = resolveState(width, height);
        if (state == null) {
            return;
        }
        renderTextContent(guiGraphics, state);
    }

    public final void renderBackground(int mouseX, int mouseY, int width, int height) {
        AnimationState state = resolveState(width, height);
        if (state == null) {
            return;
        }

        RenderPipeline pipeline = getPipeline();
        if (pipeline == null) {
            return;
        }

        renderShaderBackground(pipeline, state, mouseX, mouseY, width, height);
    }

    private AnimationState resolveState(int screenWidth, int screenHeight) {
        if (!active) {
            return null;
        }

        AnimationState state = calculateAnimationState(screenWidth, screenHeight);
        if (!state.shouldRender()) {
            active = false;
            onHide(Minecraft.getInstance());
            return null;
        }
        return state;
    }

    private AnimationState calculateAnimationState(int screenWidth, int screenHeight) {
        long now = Util.getMillis();
        long visibleTime = Math.max(now - startTime, 0L);

        boolean leaving = visibleTime >= showDuration;
        long fadeTime = leaving ? visibleTime - showDuration : visibleTime;
        float fade = fadeDuration > 0L
                ? Mth.clamp((float) fadeTime / (float) fadeDuration, 0.0F, 1.0F)
                : 1.0F;
        float alpha = leaving ? 1.0F - fade : fade;

        if (leaving && alpha <= 0.0F) {
            return AnimationState.hidden();
        }

        int overlayWidth = overlayWidth();
        int overlayHeight = overlayHeight();
        float baseX = baseX(screenWidth, overlayWidth);
        float baseY = baseY(screenHeight, overlayHeight);
        float slideT = calculateSlideT(visibleTime);
        float x = baseX + (1.0F - slideT) * (float) overlayWidth;

        return new AnimationState(true, x, baseY, alpha, overlayWidth, overlayHeight);
    }

    private float calculateSlideT(long visibleTime) {
        if (slideDuration <= 0L) {
            return 1.0F;
        }

        if (visibleTime <= slideDuration) {
            return (float) visibleTime / (float) slideDuration;
        }
        if (visibleTime >= showDuration) {
            long leaveElapsed = Math.min(visibleTime - showDuration, slideDuration);
            return 1.0F - (float) leaveElapsed / (float) slideDuration;
        }
        return 1.0F;
    }

    private void renderShaderBackground(
            RenderPipeline pipeline,
            AnimationState state,
            int mouseX,
            int mouseY,
            int screenWidth,
            int screenHeight
    ) {
        float timeSeconds = (float) (Util.getMillis() % 100000L) / 1000.0F;
        float x0 = state.x;
        float y0 = state.y;
        float x1 = state.x + (float) state.overlayWidth;
        float y1 = state.y + (float) state.overlayHeight;

        var tesselator = Tesselator.getInstance();
        var bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(x0, y0, 0.0F).setUv(0.0F, 0.0F);
        bufferBuilder.addVertex(x0, y1, 0.0F).setUv(0.0F, 1.0F);
        bufferBuilder.addVertex(x1, y1, 0.0F).setUv(1.0F, 1.0F);
        bufferBuilder.addVertex(x1, y0, 0.0F).setUv(1.0F, 0.0F);

        var target = Minecraft.getInstance().getMainRenderTarget();
        try (MeshData meshData = bufferBuilder.buildOrThrow();
             var vertexBuffer = RenderSystem.getDevice().createBuffer(
                     () -> "oelib_overlay_vertex",
                     GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                     meshData.vertexBuffer()
             );
             var timeBuffer = createUniformBuffer(
                     "oelib_overlay_time",
                     Std140SizeCalculator::putVec4,
                     builder -> builder.putVec4(timeSeconds, 0.0F, 0.0F, 0.0F)
             );
             var resolutionBuffer = createUniformBuffer(
                     "oelib_overlay_resolution",
                     Std140SizeCalculator::putVec4,
                     builder -> builder.putVec4((float) state.overlayWidth, (float) state.overlayHeight, 0.0F, 0.0F)
             );
             var alphaBuffer = createUniformBuffer(
                     "oelib_overlay_alpha",
                     Std140SizeCalculator::putVec4,
                     builder -> builder.putVec4(state.alpha, 0.0F, 0.0F, 0.0F)
             )) {
            ByteBuffer indexData = meshData.indexBuffer();
            try (GpuBuffer customIndexBuffer = indexData != null
                    ? RenderSystem.getDevice().createBuffer(
                    () -> "oelib_overlay_index",
                    GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                    indexData
            )
                    : null) {
                boolean useMouseUniform = hasMouseUniform();
                float[] mouseUv = useMouseUniform
                        ? mouseUv(state.overlayWidth, state.overlayHeight, mouseX, mouseY, screenWidth, screenHeight)
                        : null;
                var dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                        new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F),
                        WHITE_COLOR,
                        ZERO_OFFSET,
                        IDENTITY_TEXTURE_MATRIX
                );

                try (GpuBuffer mouseBuffer = useMouseUniform
                        ? createUniformBuffer(
                        "oelib_overlay_mouse",
                        Std140SizeCalculator::putVec4,
                        builder -> builder.putVec4(mouseUv[0], mouseUv[1], 0.0F, 0.0F)
                )
                        : null) {
                    var encoder = RenderSystem.getDevice().createCommandEncoder();
                    try (var pass = encoder.createRenderPass(
                            () -> "oelib_overlay_pass",
                            target.getColorTextureView(),
                            OptionalInt.empty(),
                            target.getDepthTextureView(),
                            OptionalDouble.empty()
                    )) {
                        pass.setPipeline(pipeline);
                        RenderSystem.bindDefaultUniforms(pass);
                        pass.setUniform("DynamicTransforms", dynamicTransforms);
                        pass.setUniform("Time", timeBuffer);
                        pass.setUniform("Resolution", resolutionBuffer);
                        pass.setUniform("ToastAlpha", alphaBuffer);

                        GpuBuffer drawIndexBuffer;
                        VertexFormat.IndexType drawIndexType;
                        if (customIndexBuffer != null) {
                            drawIndexBuffer = customIndexBuffer;
                            drawIndexType = meshData.drawState().indexType();
                        } else {
                            var sequential = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                            drawIndexBuffer = sequential.getBuffer(meshData.drawState().indexCount());
                            drawIndexType = sequential.type();
                        }

                        if (mouseBuffer != null) {
                            pass.setUniform("MouseUV", mouseBuffer);
                        }
                        pass.setVertexBuffer(0, vertexBuffer);
                        pass.setIndexBuffer(drawIndexBuffer, drawIndexType);
                        pass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                    }
                }
            }
        }
    }

    private void renderTextContent(GuiGraphicsExtractor guiGraphics, AnimationState state) {
        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        var lines = textLines(minecraft);
        if (lines.length == 0) {
            return;
        }

        int color = textColor();
        float centerX = state.x + (float) state.overlayWidth / 2.0F;
        float innerWidth = Math.max(0.0F, (float) state.overlayWidth - horizontalPadding() * 2.0F);
        float titleScale = titleScale();
        float bodyScale = bodyScale();
        float currentY = state.y + 8.0F;

        drawScaledCenteredString(guiGraphics, font, lines[0], centerX, currentY, color, titleScale);
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
            drawScaledCenteredString(guiGraphics, font, lines[i], centerX, currentY, color, bodyScale);
            currentY += (float) font.lineHeight * bodyScale + 2.0F;
        }
    }

    private record AnimationState(boolean shouldRender, float x, float y, float alpha, int overlayWidth, int overlayHeight) {
        private static AnimationState hidden() {
            return new AnimationState(false, 0.0F, 0.0F, 0.0F, 0, 0);
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

    private void drawScaledCenteredString(
            GuiGraphicsExtractor guiGraphics,
            Font font,
            Component text,
            float centerX,
            float y,
            int color,
            float scale
    ) {
        var pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(centerX, y);
        pose.scale(scale, scale);
        guiGraphics.centeredText(font, text, 0, 0, color);
        pose.popMatrix();
    }

    protected boolean hasMouseUniform() {
        return false;
    }

    protected float[] mouseUv(int overlayWidth, int overlayHeight, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        return new float[]{-1.0F, -1.0F};
    }

    private static GpuBuffer createUniformBuffer(
            String label,
            Consumer<Std140SizeCalculator> sizeWriter,
            Consumer<Std140Builder> valueWriter
    ) {
        Std140SizeCalculator sizeCalculator = new Std140SizeCalculator();
        sizeWriter.accept(sizeCalculator);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(sizeCalculator.get()).order(ByteOrder.nativeOrder());
        Std140Builder builder = Std140Builder.intoBuffer(byteBuffer);
        valueWriter.accept(builder);
        byteBuffer.rewind();
        return RenderSystem.getDevice().createBuffer(
                () -> label,
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                byteBuffer
        );
    }

    protected abstract RenderPipeline getPipeline();

    protected abstract int overlayWidth();

    protected abstract int overlayHeight();
}
