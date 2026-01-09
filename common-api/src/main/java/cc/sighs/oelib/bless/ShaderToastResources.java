package cc.sighs.oelib.bless;

import net.minecraft.client.renderer.ShaderInstance;

public class ShaderToastResources {
    private static ShaderInstance spiritToastShader;
    private static ShaderInstance newYearShader;
    private static ShaderInstance valentineShader;

    private ShaderToastResources() {
    }

    public static ShaderInstance getSpiritToastShader() {
        return spiritToastShader;
    }

    static void setSpiritToastShader(ShaderInstance shader) {
        spiritToastShader = shader;
    }

    public static ShaderInstance getNewYearShader() {
        return newYearShader;
    }

    public static void setNewYearShader(ShaderInstance shader) {
        newYearShader = shader;
    }

    public static ShaderInstance getValentineShader() {
        return valentineShader;
    }

    public static void setValentineShader(ShaderInstance shader) {
        valentineShader = shader;
    }
}
