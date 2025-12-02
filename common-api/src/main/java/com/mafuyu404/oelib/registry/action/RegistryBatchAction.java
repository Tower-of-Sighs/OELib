package com.mafuyu404.oelib.registry.action;

import com.mafuyu404.oelib.registry.RegisterSupplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Objects;

public record RegistryBatchAction<T>(ResourceKey<? extends Registry<T>> registryKey, String modid,
                                     List<? extends RegisterSupplier<? extends T>> entries) implements RegistrationAction {
    public RegistryBatchAction(ResourceKey<? extends Registry<T>> registryKey,
                               String modid,
                               List<? extends RegisterSupplier<? extends T>> entries) {
        this.registryKey = Objects.requireNonNull(registryKey, "registryKey");
        this.modid = Objects.requireNonNull(modid, "modid");
        this.entries = Objects.requireNonNull(entries, "entries");
    }
}