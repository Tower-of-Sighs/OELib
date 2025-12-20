package com.sighs.oelib.registry.action;

import net.minecraft.world.level.ItemLike;

import java.util.Objects;
import java.util.function.Supplier;

public record FuelAction(int time, Supplier<? extends ItemLike>[] items) implements RegistrationAction {
    public FuelAction {
        Objects.requireNonNull(items, "items");
    }
}
