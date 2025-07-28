package com.mafuyu404.oelib.functions;

import com.mafuyu404.oelib.api.ExpressionFunction;
import net.minecraftforge.fml.ModList;

/**
 * 核心表达式函数。
 * <p>
 * 提供一些常用的表达式函数。
 * </p>
 *
 * @author Flechazo
 * @since 1.0.0
 */
public class CoreFunctions {
    
    /**
     * 检查模组是否已加载。
     *
     * @param modid 模组ID
     * @return 是否已加载
     */
    @ExpressionFunction(value = "isModLoaded", description = "检查模组是否已加载", category = "mod")
    public static boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }
}