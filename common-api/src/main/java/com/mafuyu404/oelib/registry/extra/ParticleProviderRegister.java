package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.ParticleProviderAction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

@Environment(EnvType.CLIENT)
public final class ParticleProviderRegister {
    private ParticleProviderRegister() {
    }

    public static <T extends ParticleOptions> void register(ParticleType<T> type, ParticleProvider<T> provider) {
        RegistrationDispatcher.perform(new ParticleProviderAction<>(type, provider));
    }
}