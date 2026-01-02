package cc.sighs.oelib.neoforge.example;

import cc.sighs.oelib.data.DataManager;
import cc.sighs.oelib.data.api.ExpressionFunction;

public class CoreFunctions {

    @ExpressionFunction(value = "isModLoaded", description = "检查模组是否已加载", category = "mod")
    public static boolean isModLoaded(String modid) {
        return DataManager.isModLoaded(modid);
    }
}