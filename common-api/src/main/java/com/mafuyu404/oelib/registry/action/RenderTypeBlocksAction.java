package com.mafuyu404.oelib.registry.action;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

import java.util.Objects;

public record RenderTypeBlocksAction(RenderType type, Block[] blocks) implements RegistrationAction {
    public RenderTypeBlocksAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(blocks, "blocks");
    }
}