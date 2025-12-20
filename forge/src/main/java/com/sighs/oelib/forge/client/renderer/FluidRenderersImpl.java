package com.sighs.oelib.forge.client.renderer;

import com.sighs.oelib.renderer.FluidRenderers;
import com.sighs.oelib.renderer.spi.IFluidRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public final class FluidRenderersImpl implements IFluidRenderers {

    @Override
    public void render(GuiGraphics graphics, Object fluidRef, long amount, long capacity,
                       int x, int y, int width, int height) {
        if (!(fluidRef instanceof FluidStack stack)) return;
        if (stack.isEmpty() || stack.getFluid() == Fluids.EMPTY) {
            return;
        }

        IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(stack.getFluid());
        int color = props.getTintColor(stack);

        ResourceLocation stillTex = props.getStillTexture(stack);

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTex);

        if (sprite.atlasLocation() == MissingTextureAtlasSprite.getLocation()) {
            return;
        }

        int scaledHeight = FluidRenderers.computeScaledHeight(amount, capacity, height, FluidRenderers.MIN_FLUID_HEIGHT);
        FluidRenderers.renderTiledSprite(graphics, sprite, color, x, y, width, scaledHeight, height);
    }
}