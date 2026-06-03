package cc.sighs.oelib.event.fabric.impl;

import cc.sighs.oelib.OELibEvent;
import cc.sighs.oelib.event.fabric.EventPriority;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public class PriorityEventRegistry {

    private static final Map<Event<?>, PriorityEventHandler<?>> eventHandlers = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Set<RegisteredListener>> pendingRegistrations = new ConcurrentHashMap<>();

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

        OELibEvent.LOGGER.debug("Registered priority event listeners from class: {}", listenerClass.getSimpleName());
    }

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

        OELibEvent.LOGGER.debug("Registered priority listener for event {} with priority {}",
                event.getClass().getSimpleName(), priority);
    }

    public static <T> void register(Event<T> event, T listener) {
        Class<?> listenerClass = listener.getClass();
        EventPriority annotation = listenerClass.getAnnotation(EventPriority.class);

        int priority = annotation != null ? annotation.priority() : EventPriority.NORMAL;
        register(event, listener, priority);
    }

    public static <T> void registerToPhase(Event<T> event, ResourceLocation phase, T listener, int priority) {
        register(event, listener, priority);

        try {
            event.register(phase, listener);
        } catch (Exception e) {
            OELibEvent.LOGGER.debug("Event {} does not support phases, using priority only",
                    event.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> PriorityEventHandler<T> getHandler(Event<T> event) {
        return (PriorityEventHandler<T>) eventHandlers.get(event);
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean unregister(Event<T> event, T listener) {
        PriorityEventHandler<T> handler = (PriorityEventHandler<T>) eventHandlers.get(event);
        return handler != null && handler.unregister(listener);
    }

    public static void clear() {
        eventHandlers.clear();
        pendingRegistrations.clear();
        OELibEvent.LOGGER.debug("Cleared all priority event registrations");
    }

    public static String getStatistics() {
        int totalEvents = eventHandlers.size();
        int totalListeners = eventHandlers.values().stream()
                .mapToInt(PriorityEventHandler::getListenerCount)
                .sum();

        return String.format("Priority Event Registry: %d events, %d listeners", totalEvents, totalListeners);
    }

    private static void registerMethod(Method method) {
        EventPriority annotation = method.getAnnotation(EventPriority.class);
        int priority = annotation.priority();
        String description = annotation.description();

        Class<?> declaringClass = method.getDeclaringClass();
        pendingRegistrations.computeIfAbsent(declaringClass, k -> new HashSet<>())
                .add(new RegisteredListener(method, priority, description));

        OELibEvent.LOGGER.debug("Registered method {} with priority {} ({})",
                method.getName(), priority, description.isEmpty() ? "no description" : description);
    }

    private record RegisteredListener(Method method, int priority, String description) {
    }
}