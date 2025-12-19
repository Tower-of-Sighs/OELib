package com.sighs.oelib.registry.extra;

import com.sighs.oelib.registry.RegistrationDispatcher;
import com.sighs.oelib.registry.action.EntityAttributeAction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.function.Supplier;

public final class EntityAttributeRegister {
    private EntityAttributeRegister() {
    }

    public static void register(Supplier<? extends EntityType<? extends LivingEntity>> type,
                                Supplier<AttributeSupplier.Builder> attributes) {
        RegistrationDispatcher.perform(new EntityAttributeAction(type, attributes));
    }
}