package com.mafuyu404.oelib.registry.action;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.Fluid;

import java.util.Objects;
import java.util.function.Supplier;

public record RenderTypeFluidsAction(RenderType type,
                                     Supplier<? extends Fluid>[] fluids) implements RegistrationAction {
    public RenderTypeFluidsAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fluids, "fluids");
    }
}