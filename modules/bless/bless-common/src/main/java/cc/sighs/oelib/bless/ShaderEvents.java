package cc.sighs.oelib.bless;

import cc.sighs.oelib.registry.extra.ShaderRegister;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;

public class ShaderEvents {
    public static void register() {
        ShaderRegister.register(ResourceLocation.fromNamespaceAndPath(OELibBless.MOD_ID, "chongyang"),
                DefaultVertexFormat.POSITION_TEX, ShaderToastResources::setSpiritToastShader);

        ShaderRegister.register(ResourceLocation.fromNamespaceAndPath(OELibBless.MOD_ID, "new_year"),
                DefaultVertexFormat.POSITION_TEX, ShaderToastResources::setNewYearShader);

        ShaderRegister.register(ResourceLocation.fromNamespaceAndPath(OELibBless.MOD_ID, "valentine"),
                DefaultVertexFormat.POSITION_TEX, ShaderToastResources::setValentineShader);
    }
}