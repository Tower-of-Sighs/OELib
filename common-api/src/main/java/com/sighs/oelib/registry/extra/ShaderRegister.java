package com.sighs.oelib.registry.extra;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.sighs.oelib.registry.RegistrationDispatcher;
import com.sighs.oelib.registry.action.ShaderRegisterAction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class ShaderRegister {
    private ShaderRegister() {
    }

    public static void register(ResourceLocation id,
                                VertexFormat vertexFormat,
                                Consumer<ShaderInstance> loadCallback) {
        RegistrationDispatcher.perform(new ShaderRegisterAction(id, vertexFormat, loadCallback));
    }
}

