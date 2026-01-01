package com.sighs.oelib.registry.action;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Consumer;

public record ShaderRegisterAction(ResourceLocation id,
                                   VertexFormat vertexFormat,
                                   Consumer<ShaderInstance> loadCallback) implements RegistrationAction {
    public ShaderRegisterAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(vertexFormat, "vertexFormat");
        Objects.requireNonNull(loadCallback, "loadCallback");
    }
}