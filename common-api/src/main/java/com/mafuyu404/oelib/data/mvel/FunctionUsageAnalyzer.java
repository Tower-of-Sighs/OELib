package com.mafuyu404.oelib.data.mvel;

import com.mafuyu404.oelib.OELib;
import dev.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FunctionUsageAnalyzer {
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");

    public static <T> Set<String> analyzeUsedFunctions(Map<ResourceLocation, T> dataPackData,
                                                       DataExpressionExtractor<T> dataExtractor) {
        Set<String> usedFunctions = new HashSet<>();
        for (Map.Entry<ResourceLocation, T> entry : dataPackData.entrySet()) {
            T data = entry.getValue();
            if (!shouldLoadDataPackFunctions(data, entry.getKey(), dataExtractor)) {
                continue;
            }
            usedFunctions.addAll(analyzeSingleFile(data, dataExtractor));
        }
        OELib.LOGGER.debug("Found {} used functions in data packages: {}", usedFunctions.size(), usedFunctions);
        return usedFunctions;
    }

    private static <T> boolean shouldLoadDataPackFunctions(T data, ResourceLocation location,
                                                           DataExpressionExtractor<T> dataExtractor) {
        Map<String, String> vars = dataExtractor.extractVariables(data);
        if (vars == null) return true;

        String modLoadedExpression = vars.get("modLoaded");
        if (modLoadedExpression == null) return true;

        try {
            String modId = extractModIdFromExpression(modLoadedExpression);
            if (modId != null) {
                boolean isLoaded = Platform.isModLoaded(modId);
                OELib.LOGGER.debug("Mod '{}' loaded status: {} for datapack {}", modId, isLoaded, location);
                return isLoaded;
            }
        } catch (Exception e) {
            OELib.LOGGER.debug("Failed to evaluate modLoaded expression for {}: {}", location, e.getMessage());
        }
        return true;
    }

    private static String extractModIdFromExpression(String expression) {
        if (expression == null) return null;
        Pattern pattern = Pattern.compile("isModLoaded\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");
        Matcher matcher = pattern.matcher(expression);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static <T> Set<String> analyzeSingleFile(T data, DataExpressionExtractor<T> dataExtractor) {
        Set<String> functions = new HashSet<>();
        Set<String> expressions = dataExtractor.extractAllExpressions(data);
        for (String expression : expressions) {
            functions.addAll(extractFunctionsFromExpression(expression));
        }
        return functions;
    }

    public static Set<String> extractFunctionsFromExpression(String expression) {
        Set<String> functions = new HashSet<>();
        if (expression == null || expression.trim().isEmpty()) return functions;

        Matcher matcher = FUNCTION_PATTERN.matcher(expression);
        while (matcher.find()) {
            String functionName = matcher.group(1);
            if (!isKeyword(functionName)) {
                functions.add(functionName);
            }
        }
        return functions;
    }

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

    public static Set<String> getCoreRequiredFunctions() {
        return Set.of("isModLoaded");
    }

    public interface DataExpressionExtractor<T> {
        Map<String, String> extractVariables(T data);

        Set<String> extractAllExpressions(T data);
    }
}