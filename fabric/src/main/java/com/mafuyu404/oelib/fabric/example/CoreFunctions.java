package com.mafuyu404.oelib.fabric.example;

import com.mafuyu404.oelib.api.data.ExpressionFunction;
import dev.architectury.platform.Platform;

public class CoreFunctions {

    @ExpressionFunction(value = "isModLoaded", description = "检查模组是否已加载", category = "mod")
    public static boolean isModLoaded(String modid) {
        return Platform.isModLoaded(modid);
    }
}