package com.sighs.oelib.fabric.client.renderer;

import com.sighs.oelib.renderer.FluidRenderers;
import com.sighs.oelib.renderer.spi.IFluidRenderers;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class FluidRenderersImpl implements IFluidRenderers {

    @Override
    public void render(GuiGraphics graphics, Object fluidRef, long amount, long capacity,
                       int x, int y, int width, int height) {
        FluidVariant variant = fluidRef instanceof FluidVariant fv ? fv : FluidVariant.blank();
        if (variant.isBlank()) return;

        TextureAtlasSprite sprite = FluidVariantRendering.getSprite(variant);
        if (sprite == null || sprite.atlasLocation() == MissingTextureAtlasSprite.getLocation()) {
            return;
        }

        int color = FluidVariantRendering.getColor(variant);
        int scaledHeight = FluidRenderers.computeScaledHeight(amount, capacity, height, FluidRenderers.MIN_FLUID_HEIGHT);
        FluidRenderers.renderTiledSprite(graphics, sprite, color, x, y, width, scaledHeight, height);
    }
}
