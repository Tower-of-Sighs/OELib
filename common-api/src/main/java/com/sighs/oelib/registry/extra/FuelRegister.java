package com.sighs.oelib.registry.extra;

import com.sighs.oelib.registry.RegistrationDispatcher;
import com.sighs.oelib.registry.action.FuelAction;
import com.sighs.oelib.registry.spi.FuelRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ServiceLoader;

public final class FuelRegister {
    private static final FuelRegistry IMPL = ServiceLoader
            .load(FuelRegistry.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No FuelApi impl found"));

    private FuelRegister() {
    }

    public static void register(int time, ItemLike... items) {
        RegistrationDispatcher.perform(new FuelAction(time, items));
    }

    public static int get(ItemStack stack) {
        return IMPL.getBurnTime(stack);
    }
}