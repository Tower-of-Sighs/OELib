package com.mafuyu404.oelib.event;

import net.minecraftforge.eventbus.api.Event;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数注册事件。
 * <p>
 * 此事件在 OELib 初始化表达式引擎时触发，
 * 其他模组可以监听此事件来注册自己的函数类。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Mod.EventBusSubscriber(modid = "yourmod")
 * public class YourModOELibIntegration {
 *
 *     @SubscribeEvent(priority = EventPriority.HIGH)
 *     public static void onFunctionRegistration(FunctionRegistryEvent event) {
 *         event.registerFunctionClass(YourModFunctions.class, "yourmod");
 *     }
 * }
 * }</pre>
 *
 * @author Flechazo
 * @since 1.0.0
 */
public class FunctionRegistryEvent extends Event {
    
    private final List<Pair<Class<?>, String>> registered = new ArrayList<>();
    
    /**
     * 注册包含 {@link com.mafuyu404.oelib.api.ExpressionFunction} 注解方法的类。
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
     * 获取所有已注册的函数类列表。
     *
     * @return 已注册的函数类列表
     */
    public List<Pair<Class<?>, String>> getRegisteredClasses() {
        return registered;
    }
}