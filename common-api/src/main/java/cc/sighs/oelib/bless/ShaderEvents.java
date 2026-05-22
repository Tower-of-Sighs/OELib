package cc.sighs.oelib.bless;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.registry.extra.ShaderRegister;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;

public class ShaderEvents {
    public static void register() {
        ShaderRegister.register(new ResourceLocation(OELib.MODID, "chongyang"),
                DefaultVertexFormat.POSITION_TEX, ShaderToastResources::setSpiritToastShader);

        ShaderRegister.register(new ResourceLocation(OELib.MODID, "new_year"),
                DefaultVertexFormat.POSITION_TEX, ShaderToastResources::setNewYearShader);

        ShaderRegister.register(new ResourceLocation(OELib.MODID, "valentine"),
                DefaultVertexFormat.POSITION_TEX, ShaderToastResources::setValentineShader);
    }
}