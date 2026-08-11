package cc.sighs.oelib.registry.fabric;

import cc.sighs.oelib.registry.spi.IFuelRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.item.ItemStack;

public class FuelRegistryImpl implements IFuelRegistry {

    @Override
    public int getBurnTime(ItemStack stack) {
        var time = FuelRegistry.INSTANCE.get(stack.getItem());
        return time == null ? 0 : time;
    }
}
