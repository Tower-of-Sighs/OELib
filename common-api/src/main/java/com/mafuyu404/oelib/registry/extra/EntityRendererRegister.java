package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.EntityRendererAction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class EntityRendererRegister {
    private EntityRendererRegister() {
    }

    public static <T extends Entity> void register(Supplier<? extends EntityType<? extends T>> type,
                                                   EntityRendererProvider<T> provider) {
        RegistrationDispatcher.perform(new EntityRendererAction<>(type, provider));
    }
}