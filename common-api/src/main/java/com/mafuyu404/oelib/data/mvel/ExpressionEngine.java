package com.mafuyu404.oelib.data.mvel;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.data.mvel.gen.ExpressionFunctionRegistry;
import com.mafuyu404.oelib.data.mvel.gen.ExpressionFunctionsRegistrar;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ExpressionEngine {
    private static final Map<String, Method> functionMap = new ConcurrentHashMap<>();
    private static final Map<String, Serializable> compiledExpressions = new ConcurrentHashMap<>();
    private static ParserContext parserContext = new ParserContext();
    private static boolean initialized = false;

    private ExpressionEngine() {}

    public static void initialize() {
        initialize(null);
    }

    public static void initialize(Set<String> requiredFunctions) {
        functionMap.clear();
        compiledExpressions.clear();
        parserContext = new ParserContext();

        ServiceLoader<ExpressionFunctionsRegistrar> loader =
                ServiceLoader.load(ExpressionFunctionsRegistrar.class);

        ExpressionFunctionRegistry registry = (name, ownerClass, methodName, parameterTypes) -> {
            if (requiredFunctions != null && !requiredFunctions.contains(name)) {
                return;
            }
            try {
                Method method = ownerClass.getDeclaredMethod(methodName, parameterTypes);
                functionMap.put(name, method);
                parserContext.addImport(name, method);
                OELib.LOGGER.debug("Registered function: {} ({}#{})", name,
                        ownerClass.getSimpleName(), methodName);
            } catch (Exception e) {
                OELib.LOGGER.warn("Failed to register function {} ({}#{}): {}",
                        name, ownerClass.getName(), methodName, e.getMessage());
            }
        };

        int registrarCount = 0;
        for (ExpressionFunctionsRegistrar registrar : loader) {
            registrarCount++;
            registrar.register(registry, requiredFunctions);
        }

        // 如果定向注册没有获得任何函数，自动回退到完整注册
        if (requiredFunctions != null && functionMap.isEmpty()) {
            OELib.LOGGER.warn("No functions registered for required set {}. Falling back to full registration.", requiredFunctions);
            initialize(null);
            return;
        }

        initialized = true;
        OELib.LOGGER.info("Expression engine initialized via generated registrars: {} registrars, {} functions",
                registrarCount, functionMap.size());
    }

    public static Object evaluate(String expression, Map<String, Object> context) {
        return evaluate(expression, context, true);
    }

    public static Object evaluate(String expression, Map<String, Object> context, boolean logErrors) {
        try {
            if (!initialized) {
                initialize();
            }
            Serializable compiled = compiledExpressions.computeIfAbsent(
                    expression, expr -> MVEL.compileExpression(expr, parserContext)
            );
            return MVEL.executeExpression(compiled, context != null ? context : new HashMap<>());
        } catch (Exception e) {
            if (logErrors) {
                OELib.LOGGER.error("Failed to evaluate expression: {}", expression, e);
            }
            throw e;
        }
    }

    public static boolean isValidExpression(String expression) {
        try {
            evaluate(expression, new HashMap<>(), false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Map<String, Method> getAllFunctions() {
        return new HashMap<>(functionMap);
    }

    public static void clear() {
        functionMap.clear();
        compiledExpressions.clear();
        initialized = false;
    }

    public static void hotReload() {
        clear();
        initialize();
        OELib.LOGGER.debug("Expression engine hot reload completed");
    }

    public static Map<String, Object> createContext(Map<String, String> vars) {
        Map<String, Object> context = new HashMap<>();
        if (vars != null) {
            for (Map.Entry<String, String> var : vars.entrySet()) {
                try {
                    Object value = evaluate(var.getValue(), context, false);
                    context.put(var.getKey(), value);
                } catch (Exception e) {
                    OELib.LOGGER.debug("Failed to evaluate variable {}: {}", var.getKey(), e.getMessage());
                    context.put(var.getKey(), var.getValue());
                }
            }
        }
        return context;
    }

    public static boolean checkConditions(Map<String, String> conditions, Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) return true;

        for (Map.Entry<String, String> condition : conditions.entrySet()) {
            String key = condition.getKey();
            String expression = condition.getValue();

            Object actualValue = context.get(key);
            Object expectedValue;

            try {
                expectedValue = evaluate(expression, context, false);
            } catch (Exception e) {
                expectedValue = expression;
            }

            if (expectedValue instanceof String expectedStr && expectedStr.contains("*")) {
                String pattern = expectedStr.replace("*", ".*");
                if (actualValue == null || !Objects.equals(true, actualValue.toString().matches(pattern))) {
                    return false;
                }
            } else if (!Objects.equals(expectedValue, actualValue)) {
                return false;
            }
        }
        return true;
    }

    public static void executeActions(List<String> actions, Map<String, Object> context) {
        if (actions == null) return;
        for (String action : actions) {
            try {
                evaluate(action, context);
            } catch (Exception e) {
                OELib.LOGGER.error("Failed to execute action: {}", action, e);
            }
        }
    }

    public static boolean checkModLoadedCondition(Map<String, String> vars) {
        if (vars == null || !vars.containsKey("modLoaded")) {
            return true;
        }
        try {
            Map<String, Object> tempContext = new HashMap<>();
            Object result = evaluate(vars.get("modLoaded"), tempContext, false);
            return !Boolean.FALSE.equals(result);
        } catch (Exception e) {
            OELib.LOGGER.debug("Failed to evaluate modLoaded condition: {}", e.getMessage());
            return true;
        }
    }
}