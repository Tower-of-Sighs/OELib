package com.sighs.oelib.fabric.registry;

import com.sighs.oelib.registry.spi.FuelRegistry;
import net.minecraft.world.item.ItemStack;

public class FuelRegistryImpl implements FuelRegistry {

    @Override
    public int getBurnTime(ItemStack stack) {
        var time = net.fabricmc.fabric.api.registry.FuelRegistry.INSTANCE.get(stack.getItem());
        return time == null ? 0 : time;
    }
}
