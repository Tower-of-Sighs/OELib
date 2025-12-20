package com.sighs.oelib.registry.action;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.level.ItemLike;

import java.util.Objects;
import java.util.function.Supplier;

public record ColorItemAction(ItemColor color,
                              Supplier<? extends ItemLike>[] items) implements RegistrationAction {
    public ColorItemAction {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(items, "items");
    }
}