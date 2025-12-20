package com.sighs.oelib.registry.extra;

import com.sighs.oelib.registry.RegistrationDispatcher;
import com.sighs.oelib.registry.action.FuelAction;
import com.sighs.oelib.registry.spi.IFuelRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ServiceLoader;
import java.util.function.Supplier;

public final class FuelRegister {
    private static final IFuelRegistry IMPL = ServiceLoader
            .load(IFuelRegistry.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No FuelApi impl found"));

    private FuelRegister() {
    }

    public static void register(int time, ItemLike... items) {
        @SuppressWarnings("unchecked")
        Supplier<? extends ItemLike>[] suppliers = new Supplier[items.length];
        for (int i = 0; i < items.length; i++) {
            ItemLike item = items[i];
            suppliers[i] = () -> item;
        }
        RegistrationDispatcher.perform(new FuelAction(time, suppliers));
    }

    @SafeVarargs
    public static void register(int time, Supplier<? extends ItemLike>... items) {
        RegistrationDispatcher.perform(new FuelAction(time, items));
    }

    public static int get(ItemStack stack) {
        return IMPL.getBurnTime(stack);
    }
}
