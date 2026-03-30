package cc.sighs.oelib.renderer;

import cc.sighs.oelib.OELib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Utility class for Fluid GUI Rendering.
 * <p>
 * This class provides a platform-agnostic way to render fluids in the GUI.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 * <li>Call {@link #render(GuiGraphicsExtractor, Fluid, long, long, int, int, int, int)}
 * to draw the fluid bar with correct textures and tint colors.</li>
 * </ul>
 */
public class FluidRenderers {
    public static final int TEXTURE_SIZE = 16;
    public static final int MIN_FLUID_HEIGHT = 1;
    private static final int DEFAULT_WATER_COLOR = 0x3F76E4;
    private static boolean warnedWaterFallback;

    private FluidRenderers() {
    }

    /**
     * Renders a fluid into a rectangular area on the screen.
     *
     * @param graphics  The current GuiGraphicsExtractor instance.
     * @param fluid  The fluid to render.
     * @param amount    The current amount of fluid.
     * @param capacity  The maximum capacity of the container.
     * @param x         Target X coordinate.
     * @param y         Target Y coordinate.
     * @param width     Width of the rendering area.
     * @param height    Height of the rendering area.
     */
    public static void render(GuiGraphicsExtractor graphics, Fluid fluid, long amount, long capacity,
                              int x, int y, int width, int height) {
        var attrs = resolveDefaultAttributes(fluid);
        if (attrs == null) return;
        int scaledHeight = computeScaledHeight(amount, capacity, height, MIN_FLUID_HEIGHT);
        renderTiledSprite(graphics, attrs.sprite(), attrs.colorARGB(), x, y, width, scaledHeight, height);
    }

    public static int computeScaledHeight(long amount, long capacity, int fullHeight, int minHeight) {
        if (capacity <= 0 || fullHeight <= 0) return 0;
        long scaled = (amount * (long) fullHeight) / capacity;
        if (amount > 0 && scaled < minHeight) scaled = minHeight;
        if (scaled > fullHeight) scaled = fullHeight;
        return (int) scaled;
    }

    public static void renderTiledSprite(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite, int colorARGB,
                                         int x, int y, int width, int scaledHeight, int fullHeight) {
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
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, drawX, drawY, tileWidth, tileHeight, colorARGB);
            }
        }
    }

    public static FluidRenderAttributes resolveDefaultAttributes(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        var minecraft = Minecraft.getInstance();
        FluidModel fluidModel = minecraft.getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        if (fluidModel == null) {
            return null;
        }
        var stillMaterial = fluidModel.stillMaterial();
        if (stillMaterial == null) {
            return null;
        }
        var sprite = stillMaterial.sprite();
        if (sprite == null || sprite.atlasLocation() == MissingTextureAtlasSprite.getLocation()) {
            return null;
        }

        BlockState fluidBlockState = fluid.defaultFluidState().createLegacyBlock();
        int color = 0xFFFFFFFF;
        var tintSource = fluidModel.tintSource();
        if (tintSource != null) {
            color = tintSource.color(fluidBlockState);
            if (color == -1 && minecraft.level != null && minecraft.player != null) {
                color = tintSource.colorInWorld(fluidBlockState, minecraft.level, minecraft.player.blockPosition());
            }
            if (color == -1 && (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER)) {
                color = DEFAULT_WATER_COLOR;
                if (!warnedWaterFallback) {
                    warnedWaterFallback = true;
                    OELib.LOGGER.warn("FluidRenderers: water tint source returned -1 outside world context; fallback color 0x{} is used.",
                            Integer.toHexString(DEFAULT_WATER_COLOR));
                }
            }
        }

        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }
        return new FluidRenderAttributes(sprite, color);
    }
}
