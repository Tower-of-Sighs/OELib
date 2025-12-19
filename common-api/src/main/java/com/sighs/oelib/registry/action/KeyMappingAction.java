package com.sighs.oelib.registry.action;

import net.minecraft.client.KeyMapping;

import java.util.Objects;

public record KeyMappingAction(KeyMapping mapping) implements RegistrationAction {
    public KeyMappingAction(KeyMapping mapping) {
        this.mapping = Objects.requireNonNull(mapping, "mapping");
    }
}