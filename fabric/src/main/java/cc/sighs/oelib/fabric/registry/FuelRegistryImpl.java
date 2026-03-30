package cc.sighs.oelib.fabric.registry;

import cc.sighs.oelib.registry.spi.IFuelRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;

public class FuelRegistryImpl implements IFuelRegistry {

    @Override
    public int getBurnTime(ItemStack stack, FuelValues fuelValues) {
        return fuelValues.burnDuration(stack);
    }
}
