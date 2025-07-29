package com.mafuyu404.oelib.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件优先级注解。
 * <p>
 * 用于标记事件监听器的优先级，支持所有 Fabric 事件（包括自定义事件）。
 * 优先级数值越小，执行顺序越靠前。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class MyEventHandlers {
 *     
 *     @EventPriority(priority = EventPriority.HIGHEST)
 *     public static void onFunctionRegistry(FunctionRegistryEvent event) {
 *         // 最高优先级处理
 *         event.registerFunctionClass(CoreFunctions.class, "mymod");
 *     }
 *     
 *     @EventPriority(priority = EventPriority.LOW)
 *     public static void onServerStarted(MinecraftServer server) {
 *         // 低优先级处理
 *     }
 * }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventPriority {

    /**
     * 最高优先级 - 核心系统级别
     */
    int HIGHEST = -1000;

    /**
     * 很高优先级 - 基础框架级别
     */
    int VERY_HIGH = -750;

    /**
     * 高优先级 - 重要模组级别
     */
    int HIGH = -500;

    /**
     * 较高优先级 - 一般重要功能
     */
    int ABOVE_NORMAL = -250;

    /**
     * 普通优先级 - 默认级别
     */
    int NORMAL = 0;

    /**
     * 较低优先级 - 一般功能
     */
    int BELOW_NORMAL = 250;

    /**
     * 低优先级 - 非关键功能
     */
    int LOW = 500;

    /**
     * 很低优先级 - 装饰性功能
     */
    int VERY_LOW = 750;

    /**
     * 最低优先级 - 清理和收尾工作
     */
    int LOWEST = 1000;

    /**
     * 优先级数值。
     * <p>
     * 数值越小优先级越高，越早执行。
     * </p>
     *
     * @return 优先级数值
     */
    int priority() default NORMAL;

    /**
     * 优先级描述（可选）。
     * <p>
     * 用于调试和日志输出，帮助理解优先级设置的原因。
     * </p>
     *
     * @return 优先级描述
     */
    String description() default "";
}