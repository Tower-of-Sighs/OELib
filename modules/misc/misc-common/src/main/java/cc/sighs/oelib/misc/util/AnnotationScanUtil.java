package cc.sighs.oelib.misc.util;

import cc.sighs.oelib.misc.util.spi.IAnnotationScanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;

public final class AnnotationScanUtil {
    private static final IAnnotationScanner INSTANCE = ServiceLoader.load(IAnnotationScanner.class)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No IAnnotationScanner implementation found!"));

    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationType, Set<String> basePackages) {
        return INSTANCE.findAnnotatedClasses(annotationType, basePackages, c -> true);
    }

    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationType,
                                                     Set<String> basePackages,
                                                     Predicate<Class<?>> classFilter) {
        return INSTANCE.findAnnotatedClasses(annotationType, basePackages, classFilter);
    }

    public static Predicate<Class<?>> nonAbstractNonInterface() {
        return clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
    }
}