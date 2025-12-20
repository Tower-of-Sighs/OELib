package com.sighs.oelib.forge.example;

import com.sighs.oelib.data.DataManager;
import com.sighs.oelib.data.api.ExpressionFunction;

public class CoreFunctions {

    @ExpressionFunction(value = "isModLoaded", description = "检查模组是否已加载", category = "mod")
    public static boolean isModLoaded(String modid) {
        return DataManager.isModLoaded(modid);
    }
}