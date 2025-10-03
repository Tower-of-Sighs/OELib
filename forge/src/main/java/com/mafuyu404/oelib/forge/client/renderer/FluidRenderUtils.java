package com.mafuyu404.oelib.forge.client.renderer;

import com.mafuyu404.oelib.api.client.renderer.IFluidRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import org.joml.Matrix4f;

public class FluidRenderUtils implements IFluidRenderer<FluidStack> {
    private static final int TEXTURE_SIZE = 16;
    private static final int MIN_FLUID_HEIGHT = 1;

    private static final FluidRenderUtils INSTANCE = new FluidRenderUtils();

    public static void renderFluid(GuiGraphics graphics, FluidStack stack,
                                   int x, int y, int width, int height) {
        if (stack == null) return;
        long amount = stack.getAmount();
        long capacity = FluidType.BUCKET_VOLUME;
        INSTANCE.render(graphics, stack, amount, capacity, x, y, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, FluidStack stack, long amount, long capacity,
                       int x, int y, int width, int height) {
        if (stack.isEmpty() || stack.getFluid() == Fluids.EMPTY) {
            return;
        }

        IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(stack.getFluid());
        int color = props.getTintColor(stack);

        ResourceLocation stillTex = props.getStillTexture(stack);
        if (stillTex == null) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTex);

        if (sprite.atlasLocation() == MissingTextureAtlasSprite.getLocation()) {
            return;
        }

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

                Tesselator tess = Tesselator.getInstance();
                BufferBuilder buf = tess.getBuilder();
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