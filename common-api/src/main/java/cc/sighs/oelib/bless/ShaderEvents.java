package cc.sighs.oelib.bless;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.registry.extra.ShaderRegister;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class ShaderEvents {
    public static void register() {
        ShaderRegister.register(ResourceLocation.fromNamespaceAndPath(OELib.MODID, "chongyang"),
                DefaultVertexFormat.POSITION_TEX, ShaderToastResources::setSpiritToastShader);

        ShaderRegister.register(ResourceLocation.fromNamespaceAndPath(OELib.MODID, "new_year"),
                DefaultVertexFormat.POSITION_TEX, ShaderToastResources::setNewYearShader);

        ShaderRegister.register(ResourceLocation.fromNamespaceAndPath(OELib.MODID, "valentine"),
                DefaultVertexFormat.POSITION_TEX, ShaderToastResources::setValentineShader);
    }
}