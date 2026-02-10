package cc.sighs.oelib.fabric.event.impl;

import cc.sighs.oelib.fabric.event.EventPriority;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;

@Deprecated
public final class Events {

    private Events() {
    }

    public static <T> EventRegistrationBuilder<T> on(Event<T> event) {
        return new EventRegistrationBuilder<>(event);
    }

    public static void register(Class<?> listenerClass) {
        PriorityEventRegistry.registerClass(listenerClass);
    }

    public static class EventRegistrationBuilder<T> {
        private final Event<T> event;
        private int priority = EventPriority.NORMAL;
        private ResourceLocation phase = null;

        EventRegistrationBuilder(Event<T> event) {
            this.event = event;
        }

        public EventRegistrationBuilder<T> priority(int priority) {
            this.priority = priority;
            return this;
        }

        public EventRegistrationBuilder<T> highest() {
            return priority(EventPriority.HIGHEST);
        }

        public EventRegistrationBuilder<T> high() {
            return priority(EventPriority.HIGH);
        }

        public EventRegistrationBuilder<T> normal() {
            return priority(EventPriority.NORMAL);
        }

        public EventRegistrationBuilder<T> low() {
            return priority(EventPriority.LOW);
        }

        public EventRegistrationBuilder<T> lowest() {
            return priority(EventPriority.LOWEST);
        }

        public EventRegistrationBuilder<T> phase(ResourceLocation phase) {
            this.phase = phase;
            return this;
        }

        public void register(T listener) {
            if (phase != null) {
                PriorityEventRegistry.registerToPhase(event, phase, listener, priority);
            } else {
                PriorityEventRegistry.register(event, listener, priority);
            }
        }
    }
}