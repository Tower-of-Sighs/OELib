package cc.sighs.oelib.registry.extra;

import cc.sighs.oelib.registry.RegistrationDispatcher;
import cc.sighs.oelib.registry.action.EntityModelLayerAction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class EntityModelLayerRegister {

    public static void register(ModelLayerLocation location, Supplier<LayerDefinition> definition) {
        RegistrationDispatcher.perform(new EntityModelLayerAction(location, definition));
    }
}
