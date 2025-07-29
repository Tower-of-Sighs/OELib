package com.mafuyu404.oelib.event;

import net.fabricmc.fabric.api.event.Event;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 优先级事件处理器。
 * <p>
 * 为单个事件提供优先级支持，管理该事件的所有监听器并按优先级排序执行。
 * </p>
 *
 * <p><strong>使用注意事项：</strong></p>
 *
 * <ol>
 *   <li>
 *     <strong>监听器必须是函数式接口或 lambda 表达式：</strong><br>
 *     本系统通过 Java 动态代理（{@link java.lang.reflect.Proxy}）构建复合监听器，
 *     仅支持函数式接口（即带有 {@code @FunctionalInterface} 注解或仅包含一个抽象方法的接口）。
 *     <br><br>
 *      推荐写法：
 *     <pre>{@code
 * Events.on(PlayerBlockBreakEvents.BEFORE)
 *       .high()
 *       .register((world, player, pos, state, blockEntity) -> {
 *           // your logic here
 *           return true;
 *       });
 *     }</pre>
 *      避免使用匿名内部类或实现多个接口的对象，否则代理可能失败或行为异常。
 *   </li>
 *
 *   <li>
 *     <strong>不要多次注册同一个事件的监听器：</strong><br>
 *     每次调用 {@code register()} 都会触发复合监听器的重建。
 *     频繁调用可能导致旧 compositeListener 残留，引发重复触发或逻辑错误。
 *     <br><br>
 *      建议：每个事件仅注册一次 composite 监听器，由系统内部统一调度。
 *   </li>
 *
 *   <li>
 *     <strong>注意事件是否支持阶段（phase）：</strong><br>
 *     {@code registerToPhase()} 仅适用于支持阶段的事件（如 {@code PlayerBlockBreakEvents.AFTER}）。
 *     对不支持阶段的事件调用此方法将忽略 phase 参数，不会报错，但会记录调试日志提示。
 *   </li>
 * </ol>
 *
 * @param <T> 监听器类型
 */

public class PriorityEventHandler<T> {

    private final Event<T> event;
    private final Map<Integer, List<T>> listenersByPriority = new ConcurrentHashMap<>();
    private final Map<T, Integer> listenerPriorities = new ConcurrentHashMap<>();
    private volatile boolean needsUpdate = true;

    /**
     * 创建优先级事件处理器。
     *
     * @param event 目标事件
     */
    public PriorityEventHandler(Event<T> event) {
        this.event = event;
    }

    /**
     * 注册监听器。
     *
     * @param listener 监听器
     * @param priority 优先级
     */
    public void register(T listener, int priority) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        // 如果监听器已经注册过，先移除
        if (listenerPriorities.containsKey(listener)) {
            unregister(listener);
        }

        listenersByPriority.computeIfAbsent(priority, k -> new CopyOnWriteArrayList<>()).add(listener);
        listenerPriorities.put(listener, priority);
        needsUpdate = true;

        updateEventIfNeeded();
    }

    /**
     * 移除监听器。
     *
     * @param listener 要移除的监听器
     * @return 是否成功移除
     */
    public boolean unregister(T listener) {
        Integer priority = listenerPriorities.remove(listener);
        if (priority != null) {
            List<T> listeners = listenersByPriority.get(priority);
            if (listeners != null) {
                boolean removed = listeners.remove(listener);
                if (listeners.isEmpty()) {
                    listenersByPriority.remove(priority);
                }
                if (removed) {
                    needsUpdate = true;
                    updateEventIfNeeded();
                }
                return removed;
            }
        }
        return false;
    }

    /**
     * 获取监听器数量。
     *
     * @return 监听器数量
     */
    public int getListenerCount() {
        return listenerPriorities.size();
    }

    /**
     * 获取指定优先级的监听器数量。
     *
     * @param priority 优先级
     * @return 该优先级的监听器数量
     */
    public int getListenerCount(int priority) {
        List<T> listeners = listenersByPriority.get(priority);
        return listeners != null ? listeners.size() : 0;
    }

    /**
     * 获取所有使用的优先级。
     *
     * @return 优先级集合（已排序）
     */
    public Set<Integer> getUsedPriorities() {
        return new TreeSet<>(listenersByPriority.keySet());
    }

    /**
     * 清空所有监听器。
     */
    public void clear() {
        listenersByPriority.clear();
        listenerPriorities.clear();
        needsUpdate = true;
        updateEventIfNeeded();
    }

    /**
     * 获取按优先级排序的所有监听器。
     *
     * @return 按优先级排序的监听器列表
     */
    public List<T> getSortedListeners() {
        List<T> result = new ArrayList<>();
        
        // 按优先级排序（数值越小优先级越高）
        listenersByPriority.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.addAll(entry.getValue()));
        
        return result;
    }

    /**
     * 更新事件（如果需要）。
     */
    private void updateEventIfNeeded() {
        if (needsUpdate) {
            updateEvent();
            needsUpdate = false;
        }
    }

    /**
     * 更新事件的监听器。
     * <p>
     * 这里我们需要重新注册所有监听器到原始事件，
     * 但按照优先级顺序注册，这样 Fabric 的事件系统会按顺序执行。
     * </p>
     */
    private void updateEvent() {
        // 由于 Fabric 事件系统的限制，我们无法直接修改已注册的监听器顺序
        // 但我们可以通过创建一个包装监听器来实现优先级功能
        
        List<T> sortedListeners = getSortedListeners();
        if (!sortedListeners.isEmpty()) {
            // 创建一个复合监听器，按优先级顺序执行所有监听器
            T compositeListener = createCompositeListener(sortedListeners);
            
            // 注意：这里需要根据具体的事件类型来实现
            // 由于泛型擦除，我们需要使用反射或其他机制
            registerCompositeListener(compositeListener);
        }
    }

    /**
     * 创建复合监听器。
     * <p>
     * 这个方法需要根据具体的监听器类型来实现。
     * 由于 Java 的泛型擦除，这里使用动态代理来创建复合监听器。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private T createCompositeListener(List<T> listeners) {
        if (listeners.isEmpty()) {
            return null;
        }

        // 使用动态代理创建复合监听器
        Class<?> listenerInterface = findListenerInterface(listeners.get(0));
        
        return (T) Proxy.newProxyInstance(
                listenerInterface.getClassLoader(),
                new Class<?>[]{listenerInterface},
                (proxy, method, args) -> {
                    // 按优先级顺序执行所有监听器
                    for (T listener : listeners) {
                        try {
                            method.invoke(listener, args);
                        } catch (Exception e) {
                            // 记录错误但继续执行其他监听器
                            System.err.println("Error executing listener: " + e.getMessage());
                        }
                    }
                    return null;
                }
        );
    }

    /**
     * 查找监听器接口。
     */
    private Class<?> findListenerInterface(T listener) {
        Class<?> clazz = listener.getClass();
        
        // 查找函数式接口
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.isAnnotationPresent(FunctionalInterface.class) || 
                iface.getMethods().length == 1) {
                return iface;
            }
        }
        
        // 如果没找到，返回第一个接口
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length > 0) {
            return interfaces[0];
        }
        
        // 最后返回 Object 类
        return Object.class;
    }

    /**
     * 注册复合监听器到原始事件。
     */
    private void registerCompositeListener(T compositeListener) {
        if (compositeListener != null) {
            event.register(compositeListener);
        }
    }
}