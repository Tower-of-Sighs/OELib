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
    
    /**
     * 获取模组版本。
     *
     * @param modid 模组ID
     * @return 模组版本，如果模组未加载则返回 null
     */
    @ExpressionFunction(value = "getModVersion", description = "获取模组版本", category = "mod")
    public static String getModVersion(String modid) {
        return ModList.get().getModContainerById(modid)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse(null);
    }
    
    /**
     * 字符串是否匹配模式。
     *
     * @param str 字符串
     * @param pattern 模式（支持 * 通配符）
     * @return 是否匹配
     */
    @ExpressionFunction(value = "matches", description = "字符串是否匹配模式", category = "string")
    public static boolean matches(String str, String pattern) {
        if (str == null || pattern == null) {
            return false;
        }
        String regex = pattern.replace("*", ".*");
        return str.matches(regex);
    }
    
    /**
     * 检查字符串是否为空。
     *
     * @param str 字符串
     * @return 是否为空
     */
    @ExpressionFunction(value = "isEmpty", description = "检查字符串是否为空", category = "string")
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 检查字符串是否不为空。
     *
     * @param str 字符串
     * @return 是否不为空
     */
    @ExpressionFunction(value = "isNotEmpty", description = "检查字符串是否不为空", category = "string")
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}