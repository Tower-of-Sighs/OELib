package com.mafuyu404.oelib.core;

import com.mafuyu404.oelib.OElib;
import com.mafuyu404.oelib.api.ExpressionFunction;
import com.mafuyu404.oelib.event.FunctionRegistryEvent;
import com.mafuyu404.oelib.functions.CoreFunctions;
import com.mafuyu404.oelib.util.FunctionUsageAnalyzer;
import com.mojang.datafixers.util.Pair;
import net.neoforged.neoforge.common.NeoForge;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表达式引擎。
 * <p>
 * 负责管理和执行 MVEL 表达式，支持自定义函数注册。
 * </p>
 *
 */
public class ExpressionEngine {

    private static final Set<Class<?>> registeredClasses = ConcurrentHashMap.newKeySet();
    private static final Map<String, Method> functionMap = new ConcurrentHashMap<>();
    private static final Map<String, Serializable> compiledExpressions = new ConcurrentHashMap<>();
    private static ParserContext parserContext = new ParserContext();
    private static boolean initialized = false;

    /**
     * 注册包含 {@link ExpressionFunction} 注解方法的类。
     *
     * @param clazz 要注册的类
     * @param modid 模组ID
     */
    public static void registerFunctionClass(Class<?> clazz, String modid) {
        if (clazz == null) {
            throw new NullPointerException("Function class cannot be null");
        }
        if (modid == null) {
            throw new NullPointerException("Mod ID cannot be null");
        }

        registeredClasses.add(clazz);
        if (initialized) {
            scanClass(clazz, modid);
        }
    }

    /**
     * 初始化表达式引擎。
     */
    public static void initialize() {
        initialize(null);
    }

    /**
     * 智能初始化表达式引擎。
     *
     * @param requiredFunctions 需要的函数集合，null 表示全量注册
     */
    public static void initialize(Set<String> requiredFunctions) {
        functionMap.clear();
        compiledExpressions.clear();
        parserContext = new ParserContext();

        // 确保核心函数始终被包含在智能注册中
        if (requiredFunctions != null) {
            Set<String> allRequiredFunctions = new HashSet<>(requiredFunctions);
            allRequiredFunctions.addAll(FunctionUsageAnalyzer.getCoreRequiredFunctions());
            requiredFunctions = allRequiredFunctions;
        }

        // 触发函数注册事件
        FunctionRegistryEvent event = requiredFunctions != null ?
                new FunctionRegistryEvent(requiredFunctions) :
                new FunctionRegistryEvent();
        NeoForge.EVENT_BUS.post(event);

        // 注册核心函数类（确保始终可用）
        if (event.isSmartRegistration()) {
            scanClassSmart(CoreFunctions.class, OElib.MODID, event.getRequiredFunctions());
        } else {
            scanClass(CoreFunctions.class, OElib.MODID);
        }

        // 注册事件中收集的函数类
        for (Pair<Class<?>, String> entry : event.getRegisteredClasses()) {
            if (event.isSmartRegistration()) {
                scanClassSmart(entry.getFirst(), entry.getSecond(), event.getRequiredFunctions());
            } else {
                scanClass(entry.getFirst(), entry.getSecond());
            }
        }

        for (Class<?> clazz : registeredClasses) {
            if (clazz == CoreFunctions.class) {
                continue;
            }
            if (event.isSmartRegistration()) {
                scanClassSmart(clazz, "unknown", event.getRequiredFunctions());
            } else {
                scanClass(clazz, "unknown");
            }
        }

        initialized = true;
        OElib.LOGGER.info("Expression engine initialized with {} available functions (smart: {})",
                functionMap.size(), event.isSmartRegistration());
    }

    /**
     * 评估表达式。
     *
     * @param expression 表达式字符串
     * @param context    上下文变量
     * @return 评估结果
     */
    public static Object evaluate(String expression, Map<String, Object> context) {
        return evaluate(expression, context, true);
    }

    /**
     * 评估表达式。
     *
     * @param expression 表达式字符串
     * @param context    上下文变量
     * @param logErrors  是否记录错误日志
     * @return 评估结果
     */
    public static Object evaluate(String expression, Map<String, Object> context, boolean logErrors) {
        try {
            if (!initialized) {
                // 如果表达式引擎未初始化，只处理核心函数
                if (expression.contains("isModLoaded")) {
                    // 临时初始化只包含核心函数
                    initializeCore();
                } else {
                    if (logErrors) {
                        OElib.LOGGER.warn("Expression engine not initialized, skipping expression: {}", expression);
                    }
                    return null;
                }
            }

            Serializable compiled = compiledExpressions.computeIfAbsent(expression,
                    expr -> MVEL.compileExpression(expr, parserContext));

            return MVEL.executeExpression(compiled, context != null ? context : new HashMap<>());
        } catch (Exception e) {
            if (logErrors) {
                OElib.LOGGER.error("Failed to evaluate expression: {}", expression, e);
            }
            throw e;
        }
    }

    /**
     * 临时初始化核心函数（仅用于模组加载检查）。
     */
    private static void initializeCore() {
        if (functionMap.isEmpty()) {
            scanClass(CoreFunctions.class, OElib.MODID);
            OElib.LOGGER.debug("Initialized core functions for mod loading checks");
        }
    }

    /**
     * 检查表达式是否有效。
     *
     * @param expression 表达式字符串
     * @return 是否有效
     */
    public static boolean isValidExpression(String expression) {
        try {
            evaluate(expression, new HashMap<>(), false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取所有已注册的函数。
     *
     * @return 函数映射的副本
     */
    public static Map<String, Method> getAllFunctions() {
        return new HashMap<>(functionMap);
    }

    /**
     * 清空所有已注册的函数和类。
     */
    public static void clear() {
        functionMap.clear();
        registeredClasses.clear();
        compiledExpressions.clear();
        initialized = false;
    }

    /**
     * 热重载函数。
     */
    public static void hotReload() {
        clear();
        initialize();
        OElib.LOGGER.debug("Expression engine hot reload completed");
    }

    /**
     * 创建表达式上下文。
     * <p>
     * 将变量映射中的表达式求值并添加到上下文中。
     * </p>
     *
     * @param vars 变量映射
     * @return 上下文对象
     */
    public static Map<String, Object> createContext(Map<String, String> vars) {
        Map<String, Object> context = new HashMap<>();

        if (vars != null) {
            for (Map.Entry<String, String> var : vars.entrySet()) {
                try {
                    Object value = evaluate(var.getValue(), context, false);
                    context.put(var.getKey(), value);
                } catch (Exception e) {
                    OElib.LOGGER.debug("Failed to evaluate variable {}: {}", var.getKey(), e.getMessage());
                    context.put(var.getKey(), var.getValue()); // 使用原始字符串作为后备
                }
            }
        }

        return context;
    }

    /**
     * 检查条件是否满足。
     * <p>
     * 支持通配符匹配（*）和表达式求值。
     * </p>
     *
     * @param conditions 条件映射
     * @param context    上下文对象
     * @return 是否所有条件都满足
     */
    public static boolean checkConditions(Map<String, String> conditions, Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

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

            // 支持通配符匹配
            if (expectedValue instanceof String expectedStr && expectedStr.contains("*")) {
                String pattern = expectedStr.replace("*", ".*");
                if (actualValue == null || !actualValue.toString().matches(pattern)) {
                    return false;
                }
            } else if (!java.util.Objects.equals(expectedValue, actualValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 执行动作列表。
     * <p>
     * 依次执行动作列表中的每个表达式。
     * </p>
     *
     * @param actions 动作列表
     * @param context 上下文对象
     */
    public static void executeActions(List<String> actions, Map<String, Object> context) {
        if (actions == null) return;

        for (String action : actions) {
            try {
                evaluate(action, context);
            } catch (Exception e) {
                OElib.LOGGER.error("Failed to execute action: {}", action, e);
            }
        }
    }

    /**
     * 检查模组加载条件。
     * <p>
     * 检查变量映射中是否包含 modLoaded 条件，并验证其结果。
     * </p>
     *
     * @param vars 变量映射
     * @return 是否应该加载（true表示应该加载，false表示不应该加载）
     */
    public static boolean checkModLoadedCondition(Map<String, String> vars) {
        if (vars == null || !vars.containsKey("modLoaded")) {
            return true; // 没有条件则默认加载
        }

        try {
            Map<String, Object> tempContext = new HashMap<>();
            Object result = evaluate(vars.get("modLoaded"), tempContext, false);
            return !Boolean.FALSE.equals(result);
        } catch (Exception e) {
            OElib.LOGGER.debug("Failed to evaluate modLoaded condition: {}", e.getMessage());
            return true; // 出错时默认加载
        }
    }

    private static void scanClassSmart(Class<?> clazz, String modid, Set<String> requiredFunctions) {
        scanClassInternal(clazz, modid, requiredFunctions, true);
    }

    private static void scanClass(Class<?> clazz, String modid) {
        scanClassInternal(clazz, modid, null, false);
    }


    private static void scanClassInternal(Class<?> clazz, String modid, Set<String> requiredFunctions, boolean smart) {
        for (Method method : clazz.getDeclaredMethods()) {
            ExpressionFunction ann = method.getAnnotation(ExpressionFunction.class);
            if (ann == null) continue;

            // 验证方法必须是静态的
            if (!Modifier.isStatic(method.getModifiers())) {
                OElib.LOGGER.warn("Expression function must be static: {}.{}", clazz.getSimpleName(), method.getName());
                continue;
            }

            // 获取函数名
            String name = ann.value().isEmpty() ? method.getName() : ann.value();

            // 智能注册模式：检查是否需要该函数
            if (smart && (requiredFunctions == null || !requiredFunctions.contains(name))) {
                OElib.LOGGER.debug("Skipping unused function: {} ({})", name, clazz.getSimpleName());
                continue;
            }

            // 冲突检查
            if (functionMap.containsKey(name)) {
                Method conflict = functionMap.get(name);
                OElib.LOGGER.warn("Function name conflict: {} conflicts with {}.{}",
                        name, conflict.getDeclaringClass().getSimpleName(), conflict.getName());
                continue;
            }

            functionMap.put(name, method);
            parserContext.addImport(name, method);
            OElib.LOGGER.debug("Registered expression function{}: {} ({})",
                    smart ? " (smart)" : "", name, clazz.getSimpleName());
        }
    }
}