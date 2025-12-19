package com.sighs.oelib.neoforge.registry;

import com.sighs.oelib.registry.spi.FuelRegistry;
import net.minecraft.world.item.ItemStack;

public class FuelRegistryImpl implements FuelRegistry {
    @Override
    public int getBurnTime(ItemStack stack) {
        return stack.getBurnTime(null);
    }
}
