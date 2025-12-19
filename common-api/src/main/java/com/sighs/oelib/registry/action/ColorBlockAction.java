package com.sighs.oelib.registry.action;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.function.Supplier;

public record ColorBlockAction(BlockColor color,
                               Supplier<? extends Block>[] blocks) implements RegistrationAction {
    public ColorBlockAction {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(blocks, "blocks");
    }
}