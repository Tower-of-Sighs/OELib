package com.mafuyu404.oelib.api.item;

import net.minecraft.world.item.ItemStack;

public interface FuelApi {
    int getBurnTime(ItemStack stack);
}