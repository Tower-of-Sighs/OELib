package cc.sighs.oelib.event;

import cc.sighs.oelib.util.AnnotationScanUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Utility class for automatically registering event listeners based on
 * {@link Subscribe} annotations.
 * <p>
 * This class scans the configured base packages for classes that contain at
 * least one method annotated with {@link Subscribe}. For each such class:
 * <ul>
 *     <li>If it declares any non-static {@code @Subscribe} methods, an
 *     instance is created via a no-arg constructor and registered with
 *     {@link EventBus#register(Object)}.</li>
 *     <li>Otherwise the class itself is registered via
 *     {@link EventBus#register(Class)} for static handlers.</li>
 * </ul>
 * <p>
 * Recommended usage is to group all listener classes under one or a few
 * dedicated packages (for example, {@code your.modid.event}) and pass those
 * packages to {@link #registerBasePackage(String)} during initialization.
 */
public final class EventAutoRegistration {
    private static final Set<String> BASE_PACKAGES = new LinkedHashSet<>();

    private EventAutoRegistration() {
    }

    public static void registerBasePackage(String basePackage) {
        if (basePackage == null || basePackage.isEmpty()) {
            return;
        }
        BASE_PACKAGES.add(basePackage);
    }

    public static int registerAllListeners() {
        Set<Class<?>> classes = AnnotationScanUtil.findAnnotatedClasses(
                Subscribe.class,
                Set.copyOf(BASE_PACKAGES),
                AnnotationScanUtil.nonAbstractNonInterface()
        );
        int count = 0;
        for (Class<?> clazz : classes) {
            boolean hasInstanceMethod = false;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Subscribe.class) && !Modifier.isStatic(m.getModifiers())) {
                    hasInstanceMethod = true;
                    break;
                }
            }
            try {
                if (hasInstanceMethod) {
                    Constructor<?> ctor = clazz.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    Object instance = ctor.newInstance();
                    EventBus.register(instance);
                } else {
                    EventBus.register(clazz);
                }
                count++;
            } catch (Throwable ignored) {
            }
        }
        return count;
    }
}
