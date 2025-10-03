package com.mafuyu404.oelib.fabric.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import com.mafuyu404.oelib.api.client.renderer.IFluidRenderer;

@SuppressWarnings("UnstableApiUsage")
public class FluidRenderUtils implements IFluidRenderer<FluidVariant> {
    private static final int TEXTURE_SIZE = 16;
    private static final int MIN_FLUID_HEIGHT = 1;

    private static final FluidRenderUtils INSTANCE = new FluidRenderUtils();

    public static void renderFluid(GuiGraphics graphics, FluidVariant variant,
                                   long amount, long capacity,
                                   int x, int y, int width, int height) {
        INSTANCE.render(graphics, variant, amount, capacity, x, y, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, FluidVariant variant, long amount, long capacity,
                       int x, int y, int width, int height) {
        if (variant == null || variant.isBlank()) return;

        TextureAtlasSprite sprite = FluidVariantRendering.getSprite(variant);
        if (sprite == null || sprite.atlasLocation() == MissingTextureAtlasSprite.getLocation()) {
            return;
        }

        int color = FluidVariantRendering.getColor(variant);
        int scaledHeight = computeScaledHeight(amount, capacity, height, MIN_FLUID_HEIGHT);

        renderTiledSprite(graphics, sprite, color, x, y, width, scaledHeight, height);
    }

    @Override
    public void renderTiledSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int color,
                                  int x, int y, int width, int scaledHeight, int fullHeight) {
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderColor(
                (color >> 16 & 0xFF) / 255.0F,
                (color >> 8 & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                (color >> 24 & 0xFF) / 255.0F
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

                uMax = uMax - (float)(TEXTURE_SIZE - tileWidth) / 16F * (uMax - uMin);
                vMax = vMax - (float)(TEXTURE_SIZE - tileHeight) / 16F * (vMax - vMin);

                var tess = Tesselator.getInstance();
                var buf = tess.getBuilder();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                buf.vertex(matrix, drawX, drawY + tileHeight, 100).uv(uMin, vMax).endVertex();
                buf.vertex(matrix, drawX + tileWidth, drawY + tileHeight, 100).uv(uMax, vMax).endVertex();
                buf.vertex(matrix, drawX + tileWidth, drawY, 100).uv(uMax, vMin).endVertex();
                buf.vertex(matrix, drawX, drawY, 100).uv(uMin, vMin).endVertex();
                tess.end();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}