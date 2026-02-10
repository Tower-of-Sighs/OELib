package cc.sighs.oelib.fabric.event.impl;

import net.fabricmc.fabric.api.event.Event;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Deprecated
public class PriorityEventHandler<T> {

    private final Event<T> event;
    private final Map<Integer, List<T>> listenersByPriority = new ConcurrentHashMap<>();
    private final Map<T, Integer> listenerPriorities = new ConcurrentHashMap<>();
    private volatile boolean needsUpdate = true;

    public PriorityEventHandler(Event<T> event) {
        this.event = event;
    }

    public void register(T listener, int priority) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        if (listenerPriorities.containsKey(listener)) {
            unregister(listener);
        }

        listenersByPriority.computeIfAbsent(priority, k -> new CopyOnWriteArrayList<>()).add(listener);
        listenerPriorities.put(listener, priority);
        needsUpdate = true;

        updateEventIfNeeded();
    }

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

    public int getListenerCount() {
        return listenerPriorities.size();
    }

    public int getListenerCount(int priority) {
        List<T> listeners = listenersByPriority.get(priority);
        return listeners != null ? listeners.size() : 0;
    }

    public Set<Integer> getUsedPriorities() {
        return new TreeSet<>(listenersByPriority.keySet());
    }

    public void clear() {
        listenersByPriority.clear();
        listenerPriorities.clear();
        needsUpdate = true;
        updateEventIfNeeded();
    }

    public List<T> getSortedListeners() {
        List<T> result = new ArrayList<>();

        listenersByPriority.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.addAll(entry.getValue()));

        return result;
    }

    private void updateEventIfNeeded() {
        if (needsUpdate) {
            updateEvent();
            needsUpdate = false;
        }
    }

    private void updateEvent() {
        List<T> sortedListeners = getSortedListeners();
        if (!sortedListeners.isEmpty()) {
            T compositeListener = createCompositeListener(sortedListeners);
            registerCompositeListener(compositeListener);
        }
    }

    @SuppressWarnings("unchecked")
    private T createCompositeListener(List<T> listeners) {
        if (listeners.isEmpty()) {
            return null;
        }

        Class<?> listenerInterface = findListenerInterface(listeners.get(0));

        return (T) Proxy.newProxyInstance(
                listenerInterface.getClassLoader(),
                new Class<?>[]{listenerInterface},
                (proxy, method, args) -> {
                    for (T listener : listeners) {
                        try {
                            method.invoke(listener, args);
                        } catch (Exception e) {
                            System.err.println("Error executing listener: " + e.getMessage());
                        }
                    }
                    return null;
                }
        );
    }

    private Class<?> findListenerInterface(T listener) {
        Class<?> clazz = listener.getClass();

        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.isAnnotationPresent(FunctionalInterface.class) ||
                    iface.getMethods().length == 1) {
                return iface;
            }
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length > 0) {
            return interfaces[0];
        }

        return Object.class;
    }

    private void registerCompositeListener(T compositeListener) {
        if (compositeListener != null) {
            event.register(compositeListener);
        }
    }
}