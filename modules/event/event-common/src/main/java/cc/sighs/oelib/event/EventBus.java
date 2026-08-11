package cc.sighs.oelib.event;

import cc.sighs.oelib.platform.Platform;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OELib event bus: register listeners and post events.
 * <p>
 * How to use:
 * <ol>
 *   <li>Annotate a void method with {@link Subscribe}, taking exactly one {@link Event} parameter.</li>
 *   <li>Register an instance via {@link #register(Object)} or a class with static handlers via {@link #register(Class)}.</li>
 *   <li>Post events with {@link #post(Event)}.</li>
 * </ol>
 * Notes:
 * <ul>
 *   <li>Handlers are ordered by {@link EventPriority} only.</li>
 *   <li>For pre/post semantics, define distinct event types (e.g. MyEvent.Pre/MyEvent.Post)
 *       and post them separately.</li>
 *   <li>{@link CancellableEvent} is supported; handlers can opt into receiving cancelled events via {@link Subscribe#receiveCanceled()}.</li>
 *   <li>Posting is thread-safe; frequent register/unregister will invalidate internal caches and is best done during initialization.</li>
 * </ul>
 */
public final class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    private static final Handler[] NO_HANDLERS = new Handler[0];
    private static final EventPriority[] PRIORITIES = EventPriority.values();

    private final Map<Class<? extends Event>, Handler[]> directHandlers = new ConcurrentHashMap<>();
    private final AtomicInteger cacheEpoch = new AtomicInteger();
    private final ClassValue<CachedDispatch> dispatch = new ClassValue<>() {
        @Override
        protected CachedDispatch computeValue(@NotNull Class<?> type) {
            return new CachedDispatch();
        }
    };

    private volatile Executor asyncExecutor = ForkJoinPool.commonPool();

    private EventBus() {
    }

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public static void register(Object listener) {
        INSTANCE.registerInternal(listener);
    }

    public static void register(Class<?> listenerClass) {
        INSTANCE.registerInternal(listenerClass, null);
    }

    public static void unregister(Object listener) {
        INSTANCE.unregisterInternal(listener);
    }

    public static void unregister(Class<?> listenerClass) {
        INSTANCE.unregisterInternal(listenerClass);
    }

    /**
     * Posts an event to all registered handlers.
     *
     * @return {@code true} if the event is cancellable and was cancelled by a handler.
     */
    public static boolean post(Event event) {
        INSTANCE.postInternal(event);
        return event instanceof CancellableEvent cancellableEvent && cancellableEvent.isCanceled();
    }


    public static <E extends Event> CompletableFuture<E> postAsync(E event) {
        return INSTANCE.postAsyncInternal(event);
    }

    public static void setAsyncExecutor(Executor executor) {
        INSTANCE.setAsyncExecutorInternal(executor);
    }

    private static Handler[] insertSorted(Handler[] existing, Handler handler) {
        if (existing == null || existing.length == 0) {
            return new Handler[]{handler};
        }
        int idx = existing.length;
        for (int i = 0; i < existing.length; i++) {
            if (shouldComeBefore(handler, existing[i])) {
                idx = i;
                break;
            }
        }
        Handler[] out = new Handler[existing.length + 1];
        System.arraycopy(existing, 0, out, 0, idx);
        out[idx] = handler;
        System.arraycopy(existing, idx, out, idx + 1, existing.length - idx);
        return out;
    }

    private static boolean shouldComeBefore(Handler a, Handler b) {
        int prioCmp = Integer.compare(a.priority.ordinal(), b.priority.ordinal());
        return prioCmp < 0;
    }

    private void registerInternal(Object listener) {
        Objects.requireNonNull(listener);
        Class<?> listenerClass = listener.getClass();
        registerInternal(listenerClass, listener);
    }

    private void registerInternal(Class<?> listenerClass, Object listenerInstance) {
        Method[] methods = listenerClass.getDeclaredMethods();
        for (Method method : methods) {
            Subscribe subscribe = method.getAnnotation(Subscribe.class);
            if (subscribe == null) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                continue;
            }
            if (!void.class.equals(method.getReturnType())) {
                continue;
            }
            Class<?> eventType = parameterTypes[0];
            if (!Event.class.isAssignableFrom(eventType)) {
                continue;
            }
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            if (!isStatic && listenerInstance == null) {
                continue;
            }
            Invoker invoker = createInvoker(listenerInstance, method, isStatic);
            if (invoker == null) {
                continue;
            }
            EventSide side = subscribe.side();
            if (side == EventSide.CLIENT && Platform.isServer()) {
                continue;
            }
            if (side == EventSide.SERVER && Platform.isClient()) {
                continue;
            }
            Handler handler = new Handler(
                    listenerInstance != null ? listenerInstance : listenerClass,
                    invoker,
                    subscribe.priority(),
                    subscribe.receiveCanceled()
            );
            addHandler(eventType.asSubclass(Event.class), handler);
        }
    }

    private void unregisterInternal(Object key) {
        boolean changed = false;
        for (Map.Entry<Class<? extends Event>, Handler[]> entry : directHandlers.entrySet()) {
            Handler[] existing = entry.getValue();
            int remainingCount = 0;
            for (Handler value : existing) {
                if (!value.owner.equals(key)) {
                    remainingCount++;
                }
            }
            if (remainingCount == existing.length) {
                continue;
            }
            changed = true;
            if (remainingCount == 0) {
                directHandlers.remove(entry.getKey());
                continue;
            }
            Handler[] remaining = new Handler[remainingCount];
            int idx = 0;
            for (Handler handler : existing) {
                if (!handler.owner.equals(key)) {
                    remaining[idx++] = handler;
                }
            }
            directHandlers.put(entry.getKey(), remaining);
        }
        if (changed) {
            cacheEpoch.incrementAndGet();
        }
    }

    public <E extends Event> E postInternal(E event) {
        if (event == null) {
            return null;
        }
        Class<? extends Event> eventClass = event.getClass();
        Handler[] allHandlers = getDispatchHandlers(eventClass);
        if (allHandlers.length == 0) {
            return event;
        }
        if (!(event instanceof CancellableEvent cancellableEvent)) {
            for (Handler allHandler : allHandlers) {
                allHandler.invoker.invoke(event);
            }
            return event;
        }
        for (Handler handler : allHandlers) {
            if (cancellableEvent.isCanceled() && !handler.receiveCanceled) {
                continue;
            }
            handler.invoker.invoke(event);
        }
        return event;
    }

    // no per-phase internal dispatch
    private <E extends Event> CompletableFuture<E> postAsyncInternal(E event) {
        if (event instanceof CancellableEvent) {
            throw new IllegalStateException("postAsync does not support CancellableEvent. Use post() on main thread or design immutable, non-cancellable events for async.");
        }
        Executor executor = asyncExecutor;
        return CompletableFuture.supplyAsync(() -> {
            postInternal(event);
            return event;
        }, executor);
    }

    private void setAsyncExecutorInternal(Executor executor) {
        asyncExecutor = Objects.requireNonNull(executor);
    }

    private Handler[] getDispatchHandlers(Class<? extends Event> eventClass) {
        CachedDispatch cached = dispatch.get(eventClass);
        int epoch = cacheEpoch.get();
        if (cached.epoch == epoch) {
            return cached.handlers;
        }
        synchronized (cached) {
            if (cached.epoch == epoch) {
                return cached.handlers;
            }
            Handler[] computed = computeHandlers(eventClass);
            cached.handlers = computed;
            cached.epoch = epoch;
            return computed;
        }
    }

    private Handler[] computeHandlers(Class<? extends Event> eventClass) {
        @SuppressWarnings("unchecked")
        ArrayList<Handler>[] buckets = (ArrayList<Handler>[]) new ArrayList[PRIORITIES.length];
        // use identity semantics for Class keys; avoids Class.equals/hashCode work
        Set<Class<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<Class<?>> stack = new ArrayDeque<>();
        stack.push(eventClass);
        while (!stack.isEmpty()) {
            Class<?> type = stack.pop();
            if (type == null || !visited.add(type)) {
                continue;
            }
            if (Event.class.isAssignableFrom(type)) {
                Class<? extends Event> evtType = type.asSubclass(Event.class);
                Handler[] direct = directHandlers.get(evtType);
                if (direct != null) {
                    for (Handler handler : direct) {
                        int bucketIndex = handler.priority.ordinal();
                        ArrayList<Handler> bucket = buckets[bucketIndex];
                        if (bucket == null) {
                            bucket = new ArrayList<>();
                            buckets[bucketIndex] = bucket;
                        }
                        bucket.add(handler);
                    }
                }
            }
            Class<?> superClass = type.getSuperclass();
            if (superClass != null && Event.class.isAssignableFrom(superClass)) {
                stack.push(superClass);
            }
            Class<?>[] interfaces = type.getInterfaces();
            for (Class<?> iface : interfaces) {
                if (Event.class.isAssignableFrom(iface)) {
                    stack.push(iface);
                }
            }
        }

        int total = 0;
        for (List<Handler> bucket : buckets) {
            if (bucket != null) {
                total += bucket.size();
            }
        }
        if (total == 0) {
            return NO_HANDLERS;
        }
        Handler[] out = new Handler[total];
        int idx = 0;
        // flatten (phase, priority) buckets in a fixed order for cache-friendly iteration
        for (int prio = 0; prio < PRIORITIES.length; prio++) {
            int bucketIndex = prio;
            List<Handler> bucket = buckets[bucketIndex];
            if (bucket == null) {
                continue;
            }
            for (Handler handler : bucket) {
                out[idx++] = handler;
            }
        }
        return out;
    }

    private void addHandler(Class<? extends Event> eventType, Handler handler) {
        // keep per-type array sorted (phase then priority); readers use a simple array walk
        directHandlers.compute(eventType, (type, existing) -> insertSorted(existing, handler));
        cacheEpoch.incrementAndGet();
    }

    private Invoker createInvoker(Object listener, Method method, boolean isStatic) {
        try {
            method.setAccessible(true);
            MethodHandles.Lookup baseLookup = MethodHandles.lookup();
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), baseLookup);
            MethodHandle handle = lookup.unreflect(method);
            if (!isStatic) {
                handle = handle.bindTo(listener);
            }
            MethodType invokedType = MethodType.methodType(Invoker.class);
            MethodType samMethodType = MethodType.methodType(void.class, Event.class);
            MethodType implMethodType = handle.type();
            CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    "invoke",
                    invokedType,
                    samMethodType,
                    handle,
                    implMethodType
            );
            Object target = site.getTarget().invokeExact();
            if (target instanceof Invoker invoker) {
                return invoker;
            }
            return null;
        } catch (Throwable t) {
            // reflective invocation as a last resort; slower but guarantees correctness
            return event -> {
                try {
                    if (isStatic) {
                        method.invoke(null, event);
                    } else {
                        method.invoke(listener, event);
                    }
                } catch (Exception ignored) {
                }
            };
        }
    }

    @FunctionalInterface
    private interface Invoker {
        void invoke(Event event);
    }

    private static final class CachedDispatch {
        private volatile int epoch = -1;
        private volatile Handler[] handlers = NO_HANDLERS;
    }

    private record Handler(Object owner, Invoker invoker, EventPriority priority,
                           boolean receiveCanceled) {
    }
}
