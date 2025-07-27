package com.mafuyu404.oelib.core;

import com.mafuyu404.oelib.OElib;
import com.mafuyu404.oelib.api.ExpressionFunction;
import com.mafuyu404.oelib.event.FunctionRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.tuple.Pair;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表达式引擎。
 * <p>
 * 负责管理和执行 MVEL 表达式，支持自定义函数注册。
 * </p>
 *
 * @author Flechazo
 * @since 1.0.0
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
        functionMap.clear();
        compiledExpressions.clear();
        parserContext = new ParserContext();
        
        // 触发函数注册事件
        FunctionRegistryEvent event = new FunctionRegistryEvent();
        MinecraftForge.EVENT_BUS.post(event);
        
        // 注册事件中收集的函数类
        for (Pair<Class<?>, String> entry : event.getRegisteredClasses()) {
            registerFunctionClass(entry.getLeft(), entry.getRight());
        }
        
        // 扫描所有已注册的类
        for (Class<?> clazz : registeredClasses) {
            scanClass(clazz, "unknown");
        }
        
        initialized = true;
        OElib.LOGGER.debug("Expression engine initialized with {} available functions", functionMap.size());
    }
    
    /**
     * 评估表达式。
     *
     * @param expression 表达式字符串
     * @param context 上下文变量
     * @return 评估结果
     */
    public static Object evaluate(String expression, Map<String, Object> context) {
        return evaluate(expression, context, true);
    }
    
    /**
     * 评估表达式。
     *
     * @param expression 表达式字符串
     * @param context 上下文变量
     * @param logErrors 是否记录错误日志
     * @return 评估结果
     */
    public static Object evaluate(String expression, Map<String, Object> context, boolean logErrors) {
        try {
            if (!initialized) {
                initialize();
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
    
    private static void scanClass(Class<?> clazz, String modid) {
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
            
            // 检查函数名冲突
            if (functionMap.containsKey(name)) {
                Method conflict = functionMap.get(name);
                OElib.LOGGER.warn("Function name conflict: {} conflicts with {}.{}", 
                        name, conflict.getDeclaringClass().getSimpleName(), conflict.getName());
                continue;
            }
            
            functionMap.put(name, method);
            parserContext.addImport(name, method);
            OElib.LOGGER.debug("Registered expression function: {} ({})", name, clazz.getSimpleName());
        }
    }
}