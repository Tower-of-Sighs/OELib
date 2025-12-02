package com.mafuyu404.oelib.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class RegisterSupplier<T> implements Supplier<T> {
    private final ResourceLocation id;
    private final Supplier<? extends T> creator;
    private final AtomicReference<Supplier<? extends T>> getterRef = new AtomicReference<>();

    public RegisterSupplier(ResourceLocation id, Supplier<? extends T> creator) {
        this.id = Objects.requireNonNull(id, "id");
        this.creator = Objects.requireNonNull(creator, "creator");
        this.getterRef.set(this.creator);
    }

    public ResourceLocation id() {
        return id;
    }

    public Supplier<? extends T> getCreator() {
        return creator;
    }

    public void setGetter(Supplier<? extends T> getter) {
        this.getterRef.set(Objects.requireNonNull(getter, "getter"));
    }

    public void bindInstance(T instance) {
        this.getterRef.set(() -> instance);
    }

    @Override
    public T get() {
        return getterRef.get().get();
    }
}