package cc.sighs.oelib.renderer.spi;

import cc.sighs.oelib.renderer.FluidRef;
import cc.sighs.oelib.renderer.FluidRenderAttributes;

public interface IFluidRenderers {
    FluidRenderAttributes resolve(FluidRef ref);
}