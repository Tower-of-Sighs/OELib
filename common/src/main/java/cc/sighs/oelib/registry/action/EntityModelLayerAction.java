package cc.sighs.oelib.registry.action;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import java.util.Objects;
import java.util.function.Supplier;

public record EntityModelLayerAction(ModelLayerLocation location,
                                     Supplier<LayerDefinition> definition) implements RegistrationAction {
    public EntityModelLayerAction {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(definition, "definition");
    }
}