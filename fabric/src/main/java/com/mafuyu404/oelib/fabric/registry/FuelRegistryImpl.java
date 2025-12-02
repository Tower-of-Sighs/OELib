package com.mafuyu404.oelib.fabric.registry;

import com.mafuyu404.oelib.api.item.FuelRegistry;
import net.minecraft.world.item.ItemStack;

public class FuelRegistryImpl implements FuelRegistry {

    @Override
    public int getBurnTime(ItemStack stack) {
        var time = net.fabricmc.fabric.api.registry.FuelRegistry.INSTANCE.get(stack.getItem());
        return time == null ? 0 : time;
    }
}
