package com.mafuyu404.oelib.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 函数注册事件。
 * <p>
 * 此事件在 OELib 初始化表达式引擎时触发，
 * 支持智能注册（只注册实际使用的函数）和全量注册。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class YourModOELibIntegration {
 *
 *     public static void init() {
 *         FunctionRegistryEvent.EVENT.register((event) -> {
 *             if (event.isSmartRegistration()) {
 *                 // 智能注册：只注册需要的函数
 *                 event.registerFunctionClassSmart(YourModFunctions.class, "yourmod");
 *             } else {
 *                 // 全量注册：注册所有函数
 *                 event.registerFunctionClass(YourModFunctions.class, "yourmod");
 *             }
 *         });
 *     }
 * }
 * }</pre>
 */
public class FunctionRegistryEvent {

    public static final Event<FunctionRegistryCallback> EVENT = EventFactory.createArrayBacked(FunctionRegistryCallback.class,
            (listeners) -> (event) -> {
                for (FunctionRegistryCallback listener : listeners) {
                    listener.onFunctionRegistry(event);
                }
            });

    private final List<Pair<Class<?>, String>> registered = new ArrayList<>();
    private final Set<String> requiredFunctions;
    private final boolean smartRegistration;

    /**
     * 创建全量注册事件。
     */
    public FunctionRegistryEvent() {
        this.requiredFunctions = Collections.emptySet();
        this.smartRegistration = false;
    }

    /**
     * 创建智能注册事件。
     *
     * @param requiredFunctions 需要的函数名集合
     */
    public FunctionRegistryEvent(Set<String> requiredFunctions) {
        this.requiredFunctions = requiredFunctions != null ? requiredFunctions : Collections.emptySet();
        this.smartRegistration = true;
    }

    /**
     * 是否为智能注册模式。
     *
     * @return true 表示智能注册，false 表示全量注册
     */
    public boolean isSmartRegistration() {
        return smartRegistration;
    }

    /**
     * 获取需要的函数名集合（仅在智能注册模式下有效）。
     *
     * @return 需要的函数名集合
     */
    public Set<String> getRequiredFunctions() {
        return requiredFunctions;
    }

    /**
     * 注册包含 {@link com.mafuyu404.oelib.api.ExpressionFunction} 注解方法的类。
     * <p>
     * 全量注册模式：注册类中的所有函数。
     * </p>
     *
     * @param clazz 要注册的类
     * @param modid 模组ID
     */
    public void registerFunctionClass(Class<?> clazz, String modid) {
        if (clazz == null) {
            throw new NullPointerException("Function class cannot be null");
        }
        if (modid == null) {
            throw new NullPointerException("Mod ID cannot be null");
        }
        registered.add(Pair.of(clazz, modid));
    }

    /**
     * 智能注册函数类。
     * <p>
     * 智能注册模式：只注册需要的函数。
     * 全量注册模式：等同于 registerFunctionClass。
     * </p>
     *
     * @param clazz 要注册的类
     * @param modid 模组ID
     */
    public void registerFunctionClassSmart(Class<?> clazz, String modid) {
        // 在智能注册模式下，ExpressionEngine 会根据 requiredFunctions 过滤
        // 这里仍然注册整个类，但 ExpressionEngine 会智能处理
        registerFunctionClass(clazz, modid);
    }

    /**
     * 检查是否需要指定的函数。
     *
     * @param functionName 函数名
     * @return 是否需要该函数
     */
    public boolean isFunctionRequired(String functionName) {
        if (!smartRegistration) {
            return true; // 全量注册模式下所有函数都需要
        }
        return requiredFunctions.contains(functionName);
    }

    /**
     * 获取所有已注册的函数类列表。
     *
     * @return 已注册的函数类列表
     */
    public List<Pair<Class<?>, String>> getRegisteredClasses() {
        return registered;
    }

    /**
     * 函数注册回调接口。
     */
    @FunctionalInterface
    public interface FunctionRegistryCallback {
        void onFunctionRegistry(FunctionRegistryEvent event);
    }
}