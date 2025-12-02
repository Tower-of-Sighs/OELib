package com.mafuyu404.oelib.api.item;

import net.minecraft.world.item.ItemStack;

public interface FuelRegistry {
    int getBurnTime(ItemStack stack);
}