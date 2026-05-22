package cc.sighs.oelib.registry.extra;

import cc.sighs.oelib.registry.RegistrationDispatcher;
import cc.sighs.oelib.registry.action.ShaderRegisterAction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class ShaderRegister {
    private ShaderRegister() {
    }

    public static void register(ResourceLocation id,
                                VertexFormat vertexFormat,
                                Consumer<ShaderInstance> loadCallback) {
        RegistrationDispatcher.perform(new ShaderRegisterAction(id, vertexFormat, loadCallback));
    }
}

