package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.EntityModelLayerAction;
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
