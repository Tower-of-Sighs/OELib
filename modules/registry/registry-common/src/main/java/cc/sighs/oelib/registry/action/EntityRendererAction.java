package cc.sighs.oelib.registry.action;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;
import java.util.function.Supplier;

public record EntityRendererAction<T extends Entity>(Supplier<? extends EntityType<? extends T>> type,
                                                     EntityRendererProvider<T> provider) implements RegistrationAction {
    public EntityRendererAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(provider, "provider");
    }
}