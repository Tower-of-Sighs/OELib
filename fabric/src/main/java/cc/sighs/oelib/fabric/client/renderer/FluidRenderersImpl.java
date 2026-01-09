package cc.sighs.oelib.fabric.client.renderer;

import cc.sighs.oelib.renderer.FluidRef;
import cc.sighs.oelib.renderer.FluidRenderAttributes;
import cc.sighs.oelib.renderer.spi.IFluidRenderers;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;

public class FluidRenderersImpl implements IFluidRenderers {
    @Override
    public FluidRenderAttributes resolve(FluidRef ref) {
        var variant = FluidVariant.of(ref.getFluid());
        var sprite = FluidVariantRendering.getSprite(variant);
        if (sprite == null || sprite.atlasLocation() == MissingTextureAtlasSprite.getLocation()) {
            return null;
        }
        int color = FluidVariantRendering.getColor(variant);
        return new FluidRenderAttributes(sprite, color);
    }
}
