package cc.sighs.oelib.registry.neoforge;

import cc.sighs.oelib.registry.spi.IFuelRegistry;
import net.minecraft.world.item.ItemStack;

public class FuelRegistryImpl implements IFuelRegistry {
    @Override
    public int getBurnTime(ItemStack stack) {
        return stack.getBurnTime(null);
    }
}
