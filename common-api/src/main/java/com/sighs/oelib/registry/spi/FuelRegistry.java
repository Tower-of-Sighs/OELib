package com.sighs.oelib.registry.spi;

import net.minecraft.world.item.ItemStack;

public interface FuelRegistry {
    int getBurnTime(ItemStack stack);
}