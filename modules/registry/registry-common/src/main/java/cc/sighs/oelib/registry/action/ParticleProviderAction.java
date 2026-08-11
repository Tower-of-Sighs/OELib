package cc.sighs.oelib.registry.action;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import java.util.Objects;

public record ParticleProviderAction<T extends ParticleOptions>(ParticleType<T> type,
                                                                ParticleProvider<T> provider) implements RegistrationAction {
    public ParticleProviderAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(provider, "provider");
    }
}