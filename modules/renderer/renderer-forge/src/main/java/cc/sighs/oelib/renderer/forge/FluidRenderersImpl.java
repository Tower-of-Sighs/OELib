package cc.sighs.oelib.renderer.forge;

import cc.sighs.oelib.renderer.FluidRef;
import cc.sighs.oelib.renderer.FluidRenderAttributes;
import cc.sighs.oelib.renderer.spi.IFluidRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public final class FluidRenderersImpl implements IFluidRenderers {
    @Override
    public FluidRenderAttributes resolve(FluidRef ref) {
        var fluid = ref.getFluid();
        var props = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, 1000);
        int color = props.getTintColor(stack);
        var stillTex = props.getStillTexture(stack);
        var sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTex);
        if (sprite.atlasLocation() == MissingTextureAtlasSprite.getLocation()) {
            return null;
        }
        return new FluidRenderAttributes(sprite, color);
    }
}