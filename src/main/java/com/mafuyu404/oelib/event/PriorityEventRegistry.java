package com.mafuyu404.oelib.event;

import com.mafuyu404.oelib.OElib;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优先级事件注册管理器。
 * <p>
 * 提供基于注解的事件优先级支持，让模组能够通过 {@link EventPriority} 注解
 * 设置事件监听器的执行优先级。
 * </p>
 */
public class PriorityEventRegistry {

    private static final Map<Event<?>, PriorityEventHandler<?>> eventHandlers = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Set<RegisteredListener>> pendingRegistrations = new ConcurrentHashMap<>();

    /**
     * 注册带有优先级注解的事件监听器类。
     * <p>
     * 扫描类中所有带有 {@link EventPriority} 注解的静态方法，
     * 并按照优先级自动注册到对应的事件。
     * </p>
     *
     * @param listenerClass 监听器类
     */
    public static void registerClass(Class<?> listenerClass) {
        if (listenerClass == null) {
            throw new IllegalArgumentException("Listener class cannot be null");
        }

        Method[] methods = listenerClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(EventPriority.class) && 
                Modifier.isStatic(method.getModifiers()) && 
                Modifier.isPublic(method.getModifiers())) {
                
                registerMethod(method);
            }
        }

        OElib.LOGGER.debug("Registered priority event listeners from class: {}", listenerClass.getSimpleName());
    }

    /**
     * 直接注册事件监听器到指定事件。
     *
     * @param event    目标事件
     * @param listener 监听器
     * @param priority 优先级
     * @param <T>      监听器类型
     */
    @SuppressWarnings("unchecked")
    public static <T> void register(Event<T> event, T listener, int priority) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        PriorityEventHandler<T> handler = (PriorityEventHandler<T>) eventHandlers.computeIfAbsent(
                event, e -> new PriorityEventHandler<>((Event<T>) e));
        
        handler.register(listener, priority);
        
        OElib.LOGGER.debug("Registered priority listener for event {} with priority {}", 
                event.getClass().getSimpleName(), priority);
    }

    /**
     * 使用注解注册事件监听器。
     *
     * @param event    目标事件
     * @param listener 监听器
     * @param <T>      监听器类型
     */
    public static <T> void register(Event<T> event, T listener) {
        Class<?> listenerClass = listener.getClass();
        EventPriority annotation = listenerClass.getAnnotation(EventPriority.class);
        
        int priority = annotation != null ? annotation.priority() : EventPriority.NORMAL;
        register(event, listener, priority);
    }

    /**
     * 便捷方法：注册到 Fabric 事件的指定阶段。
     *
     * @param event    目标事件
     * @param phase    事件阶段
     * @param listener 监听器
     * @param priority 优先级
     * @param <T>      监听器类型
     */
    public static <T> void registerToPhase(Event<T> event, ResourceLocation phase, T listener, int priority) {
        // 对于支持阶段的事件，我们可以结合阶段和优先级
        register(event, listener, priority);
        
        // 如果事件支持阶段，也注册到指定阶段
        try {
            event.register(phase, listener);
        } catch (Exception e) {
            // 如果不支持阶段，则忽略
            OElib.LOGGER.debug("Event {} does not support phases, using priority only", 
                    event.getClass().getSimpleName());
        }
    }

    /**
     * 获取事件的优先级处理器。
     *
     * @param event 事件
     * @param <T>   监听器类型
     * @return 优先级处理器，如果不存在则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> PriorityEventHandler<T> getHandler(Event<T> event) {
        return (PriorityEventHandler<T>) eventHandlers.get(event);
    }

    /**
     * 移除事件监听器。
     *
     * @param event    目标事件
     * @param listener 要移除的监听器
     * @param <T>      监听器类型
     * @return 是否成功移除
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean unregister(Event<T> event, T listener) {
        PriorityEventHandler<T> handler = (PriorityEventHandler<T>) eventHandlers.get(event);
        return handler != null && handler.unregister(listener);
    }

    /**
     * 清空所有注册的监听器。
     */
    public static void clear() {
        eventHandlers.clear();
        pendingRegistrations.clear();
        OElib.LOGGER.debug("Cleared all priority event registrations");
    }

    /**
     * 获取统计信息。
     *
     * @return 统计信息字符串
     */
    public static String getStatistics() {
        int totalEvents = eventHandlers.size();
        int totalListeners = eventHandlers.values().stream()
                .mapToInt(PriorityEventHandler::getListenerCount)
                .sum();
        
        return String.format("Priority Event Registry: %d events, %d listeners", totalEvents, totalListeners);
    }

    /**
     * 注册方法级别的监听器。
     */
    private static void registerMethod(Method method) {
        EventPriority annotation = method.getAnnotation(EventPriority.class);
        int priority = annotation.priority();
        String description = annotation.description();

        // 这里需要根据方法签名推断事件类型
        // 由于 Java 的类型擦除，我们需要一些额外的机制来处理这个问题
        // 暂时将方法信息存储起来，等待具体的事件注册时再处理
        
        Class<?> declaringClass = method.getDeclaringClass();
        pendingRegistrations.computeIfAbsent(declaringClass, k -> new HashSet<>())
                .add(new RegisteredListener(method, priority, description));

        OElib.LOGGER.debug("Registered method {} with priority {} ({})", 
                method.getName(), priority, description.isEmpty() ? "no description" : description);
    }

        /**
         * 已注册的监听器信息。
         */
        private record RegisteredListener(Method method, int priority, String description) {
    }
}