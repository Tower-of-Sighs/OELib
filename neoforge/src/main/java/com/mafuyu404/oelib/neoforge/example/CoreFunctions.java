package com.mafuyu404.oelib.neoforge.example;

import com.mafuyu404.oelib.api.data.ExpressionFunction;
import com.mafuyu404.oelib.data.DataManagerBridge;

public class CoreFunctions {

    @ExpressionFunction(value = "isModLoaded", description = "检查模组是否已加载", category = "mod")
    public static boolean isModLoaded(String modid) {
        return DataManagerBridge.isModLoaded(modid);
    }
}