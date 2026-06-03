package cc.sighs.oelib.dev.example;

import cc.sighs.oelib.data.api.ExpressionFunction;
import cc.sighs.oelib.platform.Platform;

public class CoreFunctions {

    @ExpressionFunction(value = "isModLoaded", description = "检查模组是否已加载", category = "mod")
    public static boolean isModLoaded(String modid) {
        return Platform.isModLoaded(modid);
    }
}