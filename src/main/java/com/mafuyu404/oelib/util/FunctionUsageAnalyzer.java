package com.mafuyu404.oelib.util;

import com.mafuyu404.oelib.OElib;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 函数使用分析器。
 * <p>
 * 分析数据包中使用的表达式函数，支持智能函数注册优化。
 * </p>
 */
public class FunctionUsageAnalyzer {

    private static final Pattern FUNCTION_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");

    /**
     * 分析数据包中使用的所有函数。
     *
     * @param dataPackData 数据包数据
     * @param dataExtractor 数据提取器，用于从数据对象中提取表达式
     * @param <T> 数据类型
     * @return 使用的函数名集合
     */
    public static <T> Set<String> analyzeUsedFunctions(Map<ResourceLocation, T> dataPackData,
                                                       DataExpressionExtractor<T> dataExtractor) {
        Set<String> usedFunctions = new HashSet<>();

        for (Map.Entry<ResourceLocation, T> entry : dataPackData.entrySet()) {
            T data = entry.getValue();

            // 检查模组加载状态
            if (!shouldLoadDataPackFunctions(data, entry.getKey(), dataExtractor)) {
                continue;
            }

            Set<String> fileFunctions = analyzeSingleFile(data, dataExtractor);
            usedFunctions.addAll(fileFunctions);
        }

        OElib.LOGGER.debug("Found {} used functions in data packages: {}",
                usedFunctions.size(), usedFunctions);

        return usedFunctions;
    }

    /**
     * 检查是否应该加载数据包函数。
     */
    private static <T> boolean shouldLoadDataPackFunctions(T data, ResourceLocation location,
                                                           DataExpressionExtractor<T> dataExtractor) {
        Map<String, String> vars = dataExtractor.extractVariables(data);
        if (vars == null) {
            return true;
        }

        String modLoadedExpression = vars.get("modLoaded");
        if (modLoadedExpression == null) {
            return true;
        }

        try {
            String modId = extractModIdFromExpression(modLoadedExpression);
            if (modId != null) {
                boolean isLoaded = ModList.get().isLoaded(modId);
                OElib.LOGGER.debug("Mod '{}' loaded status: {} for datapack {}", modId, isLoaded, location);
                return isLoaded;
            }
        } catch (Exception e) {
            OElib.LOGGER.debug("Failed to evaluate modLoaded expression for {}: {}", location, e.getMessage());
        }

        return true;
    }

    /**
     * 提取模组ID。
     */
    private static String extractModIdFromExpression(String expression) {
        if (expression == null) return null;

        Pattern pattern = Pattern.compile("isModLoaded\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");
        Matcher matcher = pattern.matcher(expression);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 分析单个数据包文件中使用的函数。
     */
    private static <T> Set<String> analyzeSingleFile(T data, DataExpressionExtractor<T> dataExtractor) {
        Set<String> functions = new HashSet<>();

        // 提取所有表达式
        Set<String> expressions = dataExtractor.extractAllExpressions(data);

        // 分析每个表达式中的函数
        for (String expression : expressions) {
            functions.addAll(extractFunctionsFromExpression(expression));
        }

        return functions;
    }

    /**
     * 从表达式中提取函数名。
     */
    public static Set<String> extractFunctionsFromExpression(String expression) {
        Set<String> functions = new HashSet<>();

        if (expression == null || expression.trim().isEmpty()) {
            return functions;
        }

        Matcher matcher = FUNCTION_PATTERN.matcher(expression);
        while (matcher.find()) {
            String functionName = matcher.group(1);

            if (!isKeyword(functionName)) {
                functions.add(functionName);
            }
        }

        return functions;
    }

    /**
     * 检查是否为关键字（非函数名）。
     */
    private static boolean isKeyword(String word) {
        Set<String> keywords = Set.of(
                "if", "else", "for", "while", "do", "switch", "case", "default",
                "try", "catch", "finally", "throw", "throws", "return", "break", "continue",
                "new", "this", "super", "null", "true", "false", "instanceof",
                "public", "private", "protected", "static", "final", "abstract",
                "class", "interface", "extends", "implements", "package", "import",
                "int", "long", "float", "double", "boolean", "char", "byte", "short",
                "void", "String", "Object", "List", "Map", "Set"
        );

        return keywords.contains(word);
    }

    /**
     * 必需的核心函数。
     */
    public static Set<String> getCoreRequiredFunctions() {
        return Set.of("isModLoaded");
    }

    /**
     * 数据表达式提取器接口。
     * <p>
     * 用于从不同类型的数据对象中提取表达式。
     * </p>
     *
     * @param <T> 数据类型
     */
    public interface DataExpressionExtractor<T> {
        /**
         * 提取变量映射。
         */
        Map<String, String> extractVariables(T data);

        /**
         * 提取所有表达式。
         */
        Set<String> extractAllExpressions(T data);
    }
}