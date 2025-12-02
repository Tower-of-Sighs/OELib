package com.mafuyu404.oelib.client.renderer;

import com.mafuyu404.oelib.api.client.renderer.IFluidRenderers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;

import java.util.ServiceLoader;

/**
 * 流体渲染统一入口类。
 * <p>
 * 本类提供了流体 GUI 渲染能力，通过 {@link #render(GuiGraphics, Object, long, long, int, int, int, int)}
 * 方法可自动识别传入的流体类型（{@code net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant} 或 {@code net.minecraftforge.fluids.FluidStack}），并正确渲染其纹理与颜色。
 * </p>
 *
 * <h2>基本用法</h2>
 * <ul>
 *   <li>调用 {@link #render(GuiGraphics, Object, long, long, int, int, int, int)}，
 *       传入任意平台的流体对象（如 {@code FluidVariant} 或 {@code FluidStack}）、当前流体量、总容量及目标矩形区域。</li>
 *   <li>内部会自动解析纹理 Sprite 与 ARGB 颜色，并按 {@code amount/capacity} 比例垂直缩放填充高度。</li>
 *   <li>若需手动控制渲染（例如已知 Sprite 和颜色），可直接使用 {@link #renderTiledSprite(GuiGraphics, TextureAtlasSprite, int, int, int, int, int, int)}。</li>
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