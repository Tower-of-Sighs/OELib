package com.mafuyu404.oelib.fabric.client.renderer;

import com.mafuyu404.oelib.client.renderer.FluidRenderers;
import com.mafuyu404.oelib.client.renderer.fabric.FluidRenderersImpl;
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

/**
 * @deprecated
 * See {@link FluidRenderers}
 */
@Deprecated
@SuppressWarnings("UnstableApiUsage")
public class FluidRenderUtils implements IFluidRenderer<FluidVariant> {
    private static final FluidRenderUtils INSTANCE = new FluidRenderUtils();

    public static void renderFluid(GuiGraphics graphics, FluidVariant variant,
                                   long amount, long capacity,
                                   int x, int y, int width, int height) {
        INSTANCE.render(graphics, variant, amount, capacity, x, y, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, FluidVariant variant, long amount, long capacity,
                       int x, int y, int width, int height) {
        FluidRenderersImpl.render(graphics, variant, amount, capacity, x, y, width, height);
    }

    @Override
    public void renderTiledSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int color,
                                  int x, int y, int width, int scaledHeight, int fullHeight) {
        FluidRenderers.renderTiledSprite(graphics,  sprite, color, x, y, width, scaledHeight, fullHeight);
    }
}