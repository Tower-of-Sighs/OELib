package cc.sighs.oelib.registry;

import cc.sighs.oelib.registry.action.RegistryBatchAction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class DeferredRegister<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String modid;
    private final List<RegisterSupplier<? extends T>> entries = new ArrayList<>();

    private DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String modid) {
        this.registryKey = registryKey;
        this.modid = modid;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String modid) {
        return new DeferredRegister<>(registryKey, modid);
    }

    @SuppressWarnings("unchecked")
    public <R extends T> RegisterSupplier<R> register(String name, Supplier<? extends R> supplier) {
        var id = ResourceLocation.fromNamespaceAndPath(modid, name);
        ResourceKey<? extends Registry<R>> castedRegistryKey = (ResourceKey<? extends Registry<R>>) this.registryKey;
        RegisterSupplier<R> entry = new RegisterSupplier<>(castedRegistryKey, id, supplier);
        entries.add(entry);
        return entry;
    }

    public void register() {
        RegistryBatchAction<T> action = new RegistryBatchAction<>(registryKey, modid, Collections.unmodifiableList(entries));
        RegistrationDispatcher.perform(action);
    }

    public ResourceKey<? extends Registry<T>> registryKey() {
        return registryKey;
    }

    public String modid() {
        return modid;
    }

    public List<RegisterSupplier<? extends T>> entries() {
        return Collections.unmodifiableList(entries);
    }
}