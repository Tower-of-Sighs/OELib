package cc.sighs.oelib.renderer;

import cc.sighs.oelib.renderer.spi.IFluidRenderers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix4f;

import java.util.ServiceLoader;

/**
 * Utility class for Fluid GUI Rendering.
 * <p>
 * This class provides a platform-agnostic way to render fluids in the GUI.
 * It uses {@link FluidRef} as a bridge to encapsulate platform-specific fluid representations
 * (such as Fabric's {@code FluidVariant} or NeoForge's {@code FluidStack}).
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 * <li>Implement or use an existing {@link FluidRef} to wrap your fluid data.</li>
 * <li>Call {@link #render(GuiGraphics, FluidRef, long, long, int, int, int, int)}
 * to draw the fluid bar with correct textures and tint colors.</li>
 * </ul>
 */
public class FluidRenderers {
    public static final int TEXTURE_SIZE = 16;
    public static final int MIN_FLUID_HEIGHT = 1;

    private static final IFluidRenderers IMPL;

    static {
        IMPL = ServiceLoader.load(IFluidRenderers.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No IFluidRenderers implementation found"));
    }

    private FluidRenderers() {
    }

    /**
     * Renders a fluid into a rectangular area on the screen.
     *
     * @param graphics  The current GuiGraphics instance.
     * @param ref  A bridge object containing fluid type and data.
     * @param amount    The current amount of fluid.
     * @param capacity  The maximum capacity of the container.
     * @param x         Target X coordinate.
     * @param y         Target Y coordinate.
     * @param width     Width of the rendering area.
     * @param height    Height of the rendering area.
     */
    public static void render(GuiGraphics graphics, FluidRef ref, long amount, long capacity,
                              int x, int y, int width, int height) {
        var attrs = IMPL.resolve(ref);
        if (attrs == null) return;
        int scaledHeight = computeScaledHeight(amount, capacity, height, MIN_FLUID_HEIGHT);
        renderTiledSprite(graphics, attrs.sprite(), attrs.colorARGB(), x, y, width, scaledHeight, height);
    }

    public static FluidRef of(Fluid fluid) {
        return new SimpleFluidRef(fluid);
    }

    private record SimpleFluidRef(Fluid fluid) implements FluidRef {
        @Override
            public Fluid getFluid() {
                return fluid;
            }
        }

    public static int computeScaledHeight(long amount, long capacity, int fullHeight, int minHeight) {
        if (capacity <= 0 || fullHeight <= 0) return 0;
        long scaled = (amount * (long) fullHeight) / capacity;
        if (amount > 0 && scaled < minHeight) scaled = minHeight;
        if (scaled > fullHeight) scaled = fullHeight;
        return (int) scaled;
    }

    public static void renderTiledSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int colorARGB,
                                         int x, int y, int width, int scaledHeight, int fullHeight) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.setShaderColor(
                (colorARGB >> 16 & 0xFF) / 255.0F,
                (colorARGB >> 8 & 0xFF) / 255.0F,
                (colorARGB & 0xFF) / 255.0F,
                (colorARGB >> 24 & 0xFF) / 255.0F
        );

        Matrix4f matrix = graphics.pose().last().pose();
        int yStart = y + fullHeight;

        final int xTileCount = width / TEXTURE_SIZE;
        final int xRemainder = width - (xTileCount * TEXTURE_SIZE);
        final int yTileCount = scaledHeight / TEXTURE_SIZE;
        final int yRemainder = scaledHeight - (yTileCount * TEXTURE_SIZE);

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int tileWidth = (xTile == xTileCount) ? xRemainder : TEXTURE_SIZE;
                int tileHeight = (yTile == yTileCount) ? yRemainder : TEXTURE_SIZE;
                if (tileWidth <= 0 || tileHeight <= 0) continue;

                int drawX = x + (xTile * TEXTURE_SIZE);
                int drawY = yStart - ((yTile + 1) * TEXTURE_SIZE);

                float uMin = sprite.getU0();
                float uMax = sprite.getU1();
                float vMin = sprite.getV0();
                float vMax = sprite.getV1();

                uMax = uMax - (float) (TEXTURE_SIZE - tileWidth) / 16F * (uMax - uMin);
                vMax = vMax - (float) (TEXTURE_SIZE - tileHeight) / 16F * (vMax - vMin);

                Tesselator tess = Tesselator.getInstance();
                BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

                buf.addVertex(matrix, drawX, drawY + tileHeight, 100).setUv(uMin, vMax);
                buf.addVertex(matrix, drawX + tileWidth, drawY + tileHeight, 100).setUv(uMax, vMax);
                buf.addVertex(matrix, drawX + tileWidth, drawY, 100).setUv(uMax, vMin);
                buf.addVertex(matrix, drawX, drawY, 100).setUv(uMin, vMin);
                BufferUploader.drawWithShader(buf.buildOrThrow());
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}