package com.mafuyu404.oelib.registry.action;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.Fluid;

import java.util.Objects;

public record RenderTypeFluidsAction(RenderType type, Fluid[] fluids) implements RegistrationAction {
    public RenderTypeFluidsAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fluids, "fluids");
    }
}