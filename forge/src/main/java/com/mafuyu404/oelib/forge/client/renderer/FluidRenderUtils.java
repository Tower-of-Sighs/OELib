package com.mafuyu404.oelib.forge.client.renderer;

import com.mafuyu404.oelib.api.client.renderer.IFluidRenderer;
import com.mafuyu404.oelib.client.renderer.FluidRenderers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

/**
 * @deprecated
 * See {@link FluidRenderers}
 */
@Deprecated
public class FluidRenderUtils implements IFluidRenderer<FluidStack> {

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
        FluidRenderers.render(graphics, stack, amount, capacity, x, y, width, height);
    }

    @Override
    public void renderTiledSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int color,
                                  int x, int y, int width, int scaledHeight, int fullHeight) {
        FluidRenderers.renderTiledSprite(
                graphics, sprite, color, x, y, width, scaledHeight, fullHeight
        );
    }
}