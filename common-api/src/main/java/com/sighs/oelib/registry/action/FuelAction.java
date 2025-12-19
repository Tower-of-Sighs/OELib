package com.sighs.oelib.registry.action;

import net.minecraft.world.level.ItemLike;

import java.util.Objects;

public record FuelAction(int time, ItemLike[] items) implements RegistrationAction {
    public FuelAction {
        Objects.requireNonNull(items, "items");
    }
}