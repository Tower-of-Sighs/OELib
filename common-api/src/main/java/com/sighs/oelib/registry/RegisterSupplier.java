package com.sighs.oelib.registry;

import com.sighs.oelib.registry.action.ListenAction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RegisterSupplier<T> implements Supplier<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final ResourceLocation id;
    private final Supplier<? extends T> creator;
    private final AtomicReference<Supplier<? extends T>> getterRef = new AtomicReference<>();
    private final AtomicReference<T> instanceRef = new AtomicReference<>();
    private final List<Consumer<? super T>> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean present = false;

    public RegisterSupplier(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id, Supplier<? extends T> creator) {
        this.registryKey = Objects.requireNonNull(registryKey, "registryKey");
        this.id = Objects.requireNonNull(id, "id");
        this.creator = Objects.requireNonNull(creator, "creator");
        this.getterRef.set(() -> {
            var current = instanceRef.get();
            if (current != null) return current;
            throw new IllegalStateException("Object is not registered yet: " + registryKey.location() + " " + id);
        });
    }

    public ResourceLocation id() {
        return id;
    }

    public Supplier<? extends T> getCreator() {
        return creator;
    }

    public void setGetter(Supplier<? extends T> getter) {
        Objects.requireNonNull(getter, "getter");
        this.getterRef.set(() -> {
            var current = instanceRef.get();
            if (current != null) return current;
            try {
                var v = getter.get();
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
            throw new IllegalStateException("Object is not registered yet: " + registryKey.location() + " " + id);
        });
    }

    public void bindInstance(T instance) {
        this.present = true;
        this.instanceRef.set(instance);
        this.getterRef.set(() -> instance);
        for (var l : listeners) l.accept(instance);
        listeners.clear();
    }

    @Override
    public T get() {
        return getterRef.get().get();
    }

    public ResourceKey<? extends Registry<T>> registryKey() {
        return registryKey;
    }

    public void listen(Consumer<? super T> callback) {
        Objects.requireNonNull(callback, "callback");
        if (present) {
            callback.accept(get());
        } else {
            listeners.add(callback);
            RegistrationDispatcher.perform(new ListenAction<>(registryKey, this, callback));
        }
    }

    public boolean isPresent() {
        return present;
    }

    public ResourceKey<T> key() {
        return ResourceKey.create(registryKey, id);
    }

    public void ifPresent(Consumer<? super T> action) {
        if (isPresent()) action.accept(get());
    }

    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (isPresent()) action.accept(get());
        else emptyAction.run();
    }

    public T orElse(T other) {
        return isPresent() ? get() : other;
    }

    public T orElseGet(Supplier<? extends T> supplier) {
        return isPresent() ? get() : supplier.get();
    }

    public void markPresent() {
        this.present = true;
    }
}