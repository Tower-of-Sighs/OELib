package com.mafuyu404.oelib.core;

import com.mafuyu404.oelib.OElib;
import com.mafuyu404.oelib.api.DataDriven;
import com.mafuyu404.oelib.util.FunctionUsageAnalyzer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据注册表。
 * <p>
 * 负责管理所有数据驱动类型的注册和初始化。
 * </p>
 */
public class DataRegistry {

    private static final Map<Class<?>, Integer> registeredTypes = new ConcurrentHashMap<>();
    private static final Map<Class<?>, FunctionUsageAnalyzer.DataExpressionExtractor<?>> extractors = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    private static boolean expressionEngineInitialized = false;

    /**
     * 注册数据驱动类型。
     *
     * @param dataClass 数据类型
     * @param <T>       数据类型泛型
     */
    public static <T> void register(Class<T> dataClass) {
        if (!dataClass.isAnnotationPresent(DataDriven.class)) {
            throw new IllegalArgumentException("Class " + dataClass.getSimpleName() + " must be annotated with @DataDriven");
        }

        DataDriven annotation = dataClass.getAnnotation(DataDriven.class);
        int priority = annotation.priority();

        registeredTypes.put(dataClass, priority);
        DataManager<T> manager = DataManager.register(dataClass);

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(manager);

        OElib.LOGGER.debug("Registered data-driven type: {} with priority: {}", dataClass.getSimpleName(), priority);
    }

    /**
     * 注册数据表达式提取器。
     *
     * @param dataClass 数据类型
     * @param extractor 表达式提取器
     * @param <T>       数据类型泛型
     */
    public static <T> void registerExtractor(Class<T> dataClass, FunctionUsageAnalyzer.DataExpressionExtractor<T> extractor) {
        extractors.put(dataClass, extractor);
        OElib.LOGGER.debug("Registered expression extractor for: {}", dataClass.getSimpleName());
    }

    /**
     * 初始化数据注册表。
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        OElib.LOGGER.info("Data registry initialized with {} registered types", registeredTypes.size());
    }

    /**
     * 智能初始化表达式引擎。
     * <p>
     * 在所有数据包加载完成后调用，分析所有数据包中使用的函数并进行智能注册。
     * 按照优先级顺序处理数据类型。
     * </p>
     */
    @SuppressWarnings("unchecked")
    public static void initializeExpressionEngine() {
        if (expressionEngineInitialized) {
            return;
        }

        Set<String> allUsedFunctions = new HashSet<>(FunctionUsageAnalyzer.getCoreRequiredFunctions());

        // 按优先级排序处理数据类型（数值越小优先级越高）
        List<Class<?>> sortedTypes = registeredTypes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        OElib.LOGGER.debug("Processing data types in priority order: {}",
                sortedTypes.stream()
                        .map(clazz -> clazz.getSimpleName() + "(priority:" + registeredTypes.get(clazz) + ")")
                        .collect(Collectors.joining(", ")));

        // 分析所有已注册数据类型中使用的函数
        for (Class<?> dataClass : sortedTypes) {
            FunctionUsageAnalyzer.DataExpressionExtractor<Object> extractor =
                    (FunctionUsageAnalyzer.DataExpressionExtractor<Object>) extractors.get(dataClass);

            if (extractor != null) {
                DataManager<Object> manager = (DataManager<Object>) DataManager.get(dataClass);
                if (manager != null) {
                    Map<ResourceLocation, Object> data = manager.getAllData();
                    Set<String> usedFunctions = FunctionUsageAnalyzer.analyzeUsedFunctions(data, extractor);
                    allUsedFunctions.addAll(usedFunctions);

                    OElib.LOGGER.debug("Found {} functions in {} (priority:{}): {}",
                            usedFunctions.size(), dataClass.getSimpleName(),
                            registeredTypes.get(dataClass), usedFunctions);
                }
            }
        }

        OElib.LOGGER.info("Smart registration: found {} total used functions: {}",
                allUsedFunctions.size(), allUsedFunctions);

        // 使用智能注册初始化表达式引擎
        ExpressionEngine.initialize(allUsedFunctions);

        expressionEngineInitialized = true;
    }

    /**
     * 获取所有已注册的数据类型。
     *
     * @return 已注册的数据类型集合
     */
    public static Set<Class<?>> getRegisteredTypes() {
        return Set.copyOf(registeredTypes.keySet());
    }

    /**
     * 获取按优先级排序的数据类型列表。
     *
     * @return 按优先级排序的数据类型列表（优先级数值越小越靠前）
     */
    public static List<Class<?>> getRegisteredTypesByPriority() {
        return registeredTypes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 获取数据类型的优先级。
     *
     * @param dataClass 数据类型
     * @return 优先级，如果未注册则返回 null
     */
    public static Integer getPriority(Class<?> dataClass) {
        return registeredTypes.get(dataClass);
    }

    /**
     * 检查类型是否已注册。
     *
     * @param dataClass 数据类型
     * @return 是否已注册
     */
    public static boolean isRegistered(Class<?> dataClass) {
        return registeredTypes.containsKey(dataClass);
    }

    /**
     * 检查表达式引擎是否已初始化。
     *
     * @return 是否已初始化
     */
    public static boolean isExpressionEngineInitialized() {
        return expressionEngineInitialized;
    }

    /**
     * 重置表达式引擎初始化状态（用于热重载）。
     */
    public static void resetExpressionEngine() {
        expressionEngineInitialized = false;
        ExpressionEngine.clear();
    }
}