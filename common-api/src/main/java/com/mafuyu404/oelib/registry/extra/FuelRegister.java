package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.api.item.FuelRegistry;
import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.FuelAction;
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