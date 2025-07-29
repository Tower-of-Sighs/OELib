package com.mafuyu404.oelib.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;

/**
 * 事件注册便捷 API。
 * <p>
 * 提供简洁优雅的事件注册方法，支持优先级和链式调用。
 * </p>
 */
public final class Events {

    private Events() {}

    /**
     * 开始事件注册流程。
     *
     * @param event 目标事件
     * @param <T>   监听器类型
     * @return 事件注册构建器
     */
    public static <T> EventRegistrationBuilder<T> on(Event<T> event) {
        return new EventRegistrationBuilder<>(event);
    }

    /**
     * 注册带有优先级注解的监听器类。
     *
     * @param listenerClass 监听器类
     */
    public static void register(Class<?> listenerClass) {
        PriorityEventRegistry.registerClass(listenerClass);
    }

    /**
     * 事件注册构建器。
     * <p>
     * 提供流畅的 API 来注册事件监听器。
     * </p>
     */
    public static class EventRegistrationBuilder<T> {
        private final Event<T> event;
        private int priority = EventPriority.NORMAL;
        private ResourceLocation phase = null;

        EventRegistrationBuilder(Event<T> event) {
            this.event = event;
        }

        /**
         * 设置优先级。
         *
         * @param priority 优先级
         * @return 构建器
         */
        public EventRegistrationBuilder<T> priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 设置为最高优先级。
         *
         * @return 构建器
         */
        public EventRegistrationBuilder<T> highest() {
            return priority(EventPriority.HIGHEST);
        }

        /**
         * 设置为高优先级。
         *
         * @return 构建器
         */
        public EventRegistrationBuilder<T> high() {
            return priority(EventPriority.HIGH);
        }

        /**
         * 设置为普通优先级。
         *
         * @return 构建器
         */
        public EventRegistrationBuilder<T> normal() {
            return priority(EventPriority.NORMAL);
        }

        /**
         * 设置为低优先级。
         *
         * @return 构建器
         */
        public EventRegistrationBuilder<T> low() {
            return priority(EventPriority.LOW);
        }

        /**
         * 设置为最低优先级。
         *
         * @return 构建器
         */
        public EventRegistrationBuilder<T> lowest() {
            return priority(EventPriority.LOWEST);
        }

        /**
         * 设置事件阶段。
         *
         * @param phase 事件阶段
         * @return 构建器
         */
        public EventRegistrationBuilder<T> phase(ResourceLocation phase) {
            this.phase = phase;
            return this;
        }

        /**
         * 注册监听器。
         *
         * @param listener 监听器
         */
        public void register(T listener) {
            if (phase != null) {
                PriorityEventRegistry.registerToPhase(event, phase, listener, priority);
            } else {
                PriorityEventRegistry.register(event, listener, priority);
            }
        }
    }
}