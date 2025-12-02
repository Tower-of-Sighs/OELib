package com.mafuyu404.oelib.neoforge.registry;

import com.mafuyu404.oelib.api.item.FuelRegistry;
import net.minecraft.world.item.ItemStack;

public class FuelRegistryImpl implements FuelRegistry {
    @Override
    public int getBurnTime(ItemStack stack) {
        return stack.getBurnTime(null);
    }
}
