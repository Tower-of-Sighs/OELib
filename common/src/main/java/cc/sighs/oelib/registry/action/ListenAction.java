package cc.sighs.oelib.registry.action;

import cc.sighs.oelib.registry.RegisterSupplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.function.Consumer;

public record ListenAction<T>(ResourceKey<? extends Registry<T>> registryKey,
                              RegisterSupplier<T> supplier,
                              Consumer<? super T> callback) implements RegistrationAction {
    public ListenAction {
        Objects.requireNonNull(registryKey, "registryKey");
        Objects.requireNonNull(supplier, "supplier");
        Objects.requireNonNull(callback, "callback");
    }
}