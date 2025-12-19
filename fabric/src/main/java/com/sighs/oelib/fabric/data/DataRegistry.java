package com.sighs.oelib.fabric.data;

import com.sighs.oelib.data.api.DataValidator;
import com.sighs.oelib.data.mvel.FunctionUsageAnalyzer;

import java.util.List;
import java.util.Set;

/**
 * 数据注册表。
 * <p>
 * 负责管理所有数据驱动类型的注册和初始化。
 * </p>
 */
public class DataRegistry {

    /**
     * 注册数据驱动类型。
     *
     * @param dataClass 数据类型
     * @param <T>       数据类型泛型
     */
    public static <T> void register(Class<T> dataClass) {
        com.sighs.oelib.data.DataRegistry.register(dataClass);
    }


    /**
     * 注册数据驱动类型并附加运行时命名空间。
     *
     * @param dataClass  数据类型
     * @param namespaces 要附加的命名空间
     * @param <T>        数据类型泛型
     */
    public static <T> void registerWithNamespaces(Class<T> dataClass, String... namespaces) {
        com.sighs.oelib.data.DataRegistry.registerWithNamespaces(dataClass, namespaces);
    }

    /**
     * 为数据类型注册命名空间验证器。
     * 子模组可以在各自初始化阶段调用，实现解耦。
     */
    public static <T> void registerNamespaceValidator(Class<T> dataClass, String namespace, Class<? extends DataValidator<?>> validatorClass) {
        com.sighs.oelib.data.DataRegistry.registerNamespaceValidator(dataClass, namespace, validatorClass);
    }

    /**
     * 注册数据表达式提取器。
     *
     * @param dataClass 数据类型
     * @param extractor 表达式提取器
     * @param <T>       数据类型泛型
     */
    public static <T> void registerExtractor(Class<T> dataClass, FunctionUsageAnalyzer.DataExpressionExtractor<T> extractor) {
        com.sighs.oelib.data.DataRegistry.registerExtractor(dataClass, extractor);
    }

    /**
     * 初始化数据注册表。
     */
    public static void initialize() {
        com.sighs.oelib.data.DataRegistry.initialize();
    }

    /**
     * 智能初始化表达式引擎。
     * <p>
     * 在所有数据包加载完成后调用，分析所有数据包中使用的函数并进行智能注册。
     * 按照优先级顺序处理数据类型。
     * </p>
     */
    public static void initializeExpressionEngine() {
        com.sighs.oelib.data.DataRegistry.initializeExpressionEngine();
    }

    /**
     * 获取所有已注册的数据类型。
     *
     * @return 已注册的数据类型集合
     */
    public static Set<Class<?>> getRegisteredTypes() {
        return com.sighs.oelib.data.DataRegistry.getRegisteredTypes();
    }

    /**
     * 获取按优先级排序的数据类型列表。
     *
     * @return 按优先级排序的数据类型列表（优先级数值越小越靠前）
     */
    public static List<Class<?>> getRegisteredTypesByPriority() {
        return com.sighs.oelib.data.DataRegistry.getRegisteredTypesByPriority();
    }

    /**
     * 获取数据类型的优先级。
     *
     * @param dataClass 数据类型
     * @return 优先级，如果未注册则返回 null
     */
    public static Integer getPriority(Class<?> dataClass) {
        return com.sighs.oelib.data.DataRegistry.getPriority(dataClass);
    }

    /**
     * 检查类型是否已注册。
     *
     * @param dataClass 数据类型
     * @return 是否已注册
     */
    public static boolean isRegistered(Class<?> dataClass) {
        return com.sighs.oelib.data.DataRegistry.isRegistered(dataClass);
    }

    /**
     * 检查表达式引擎是否已初始化。
     *
     * @return 是否已初始化
     */
    public static boolean isExpressionEngineInitialized() {
        return com.sighs.oelib.data.DataRegistry.isExpressionEngineInitialized();
    }

    /**
     * 重置表达式引擎初始化状态（用于热重载）。
     */
    public static void resetExpressionEngine() {
        com.sighs.oelib.data.DataRegistry.resetExpressionEngine();
    }
}