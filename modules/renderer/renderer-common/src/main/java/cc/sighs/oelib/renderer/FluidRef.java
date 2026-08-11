package cc.sighs.oelib.renderer;

import net.minecraft.world.level.material.Fluid;

/**
 * A platform-independent reference to a fluid instance.
 * <p>
 * This interface acts as a bridge between Common code and platform-specific fluid containers.
 * On Fabric, it typically wraps a {@code FluidVariant}; on NeoForge, it wraps a {@code FluidStack}.
 * </p>
 */
public interface FluidRef {
    /**
     * @return The base Minecraft {@link Fluid} type.
     */
    Fluid getFluid();
}