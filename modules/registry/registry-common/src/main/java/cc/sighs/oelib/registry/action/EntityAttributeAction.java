package cc.sighs.oelib.registry.action;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.Objects;
import java.util.function.Supplier;

public record EntityAttributeAction(Supplier<? extends EntityType<? extends LivingEntity>> type,
                                    Supplier<AttributeSupplier.Builder> attributes) implements RegistrationAction {
    public EntityAttributeAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(attributes, "attributes");
    }
}