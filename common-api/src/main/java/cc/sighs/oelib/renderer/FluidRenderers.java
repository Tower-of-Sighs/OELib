package cc.sighs.oelib.renderer;

import cc.sighs.oelib.renderer.spi.IFluidRenderers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;

import java.util.ServiceLoader;

/**
 * Utility class for Fluid GUI Rendering.
 * <p>
 * This class provides GUI rendering capabilities for fluids. The method
 * {@link #render(GuiGraphics, Object, long, long, int, int, int, int)}
 * automatically detects the type of the provided fluid object—either a
 * {@code net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant} (Fabric)
 * or a {@code net.minecraftforge.fluids.FluidStack} (Forge)—and renders it
 * with the correct texture and tint color.
 * </p>
 *
 * <h2>Basic Usage</h2>
 * <ul>
 *   <li>Call {@link #render(GuiGraphics, Object, long, long, int, int, int, int)},
 *       passing in a fluid object from either platform (e.g., {@code FluidVariant}
 *       or {@code FluidStack}), the current fluid amount, total capacity, and the
 *       target rectangular area.</li>
 *   <li>The implementation automatically resolves the appropriate texture sprite
 *       and ARGB tint color, then renders the fluid by vertically scaling its
 *       fill height according to the {@code amount / capacity} ratio.</li>
 *   <li>For fine-grained control—such as when you already have a specific sprite
 *       and color—you can use the lower-level method
 *       {@link #renderTiledSprite(GuiGraphics, TextureAtlasSprite, int, int, int, int, int, int)} directly.</li>
 * </ul>
 */
public final class FluidRenderers {
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

    public static void render(GuiGraphics graphics, Object fluidRef, long amount, long capacity,
                              int x, int y, int width, int height) {
        IMPL.render(graphics, fluidRef, amount, capacity, x, y, width, height);
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