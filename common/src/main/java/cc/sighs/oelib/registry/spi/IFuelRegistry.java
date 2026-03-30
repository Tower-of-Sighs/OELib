package cc.sighs.oelib.registry.spi;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;

public interface IFuelRegistry {
    int getBurnTime(ItemStack stack, FuelValues fuelValues);
}