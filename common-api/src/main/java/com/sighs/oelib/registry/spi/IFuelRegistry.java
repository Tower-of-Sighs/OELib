package com.sighs.oelib.registry.spi;

import net.minecraft.world.item.ItemStack;

public interface IFuelRegistry {
    int getBurnTime(ItemStack stack);
}