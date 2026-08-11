package cc.sighs.oelib.misc.util;

import cc.sighs.oelib.misc.scan.OELScanData;
import cc.sighs.oelib.misc.scan.spi.IScanDataProvider;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.effect.VTask;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class AnnotationScanUtil {
    private static final IScanDataProvider PROVIDER = ServiceLoader.load(IScanDataProvider.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No IScanDataProvider implementation found"));
    private static final ConcurrentHashMap<String, Maybe<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();

    private AnnotationScanUtil() {
    }

    public static void preload() {
        PROVIDER.preload();
    }

    public static OELScanData getScanData() {
        return PROVIDER.getScanData();
    }

    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationType) {
        return findAnnotatedClasses(annotationType, clazz -> true);
    }

    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationType,
                                                     Predicate<Class<?>> classFilter) {
        return findAnnotatedClasses(annotationType, Set.of(), classFilter);
    }

    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationType,
                                                     Set<String> basePackages) {
        return findAnnotatedClasses(annotationType, basePackages, clazz -> true);
    }

    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationType,
                                                     Set<String> basePackages,
                                                     Predicate<Class<?>> classFilter) {
        LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        getScanData().getAnnotatedBy(annotationType).stream()
                .map(OELScanData.AnnotationData::className)
                .distinct()
                .filter(className -> matchesBasePackages(className, basePackages))
                .map(className -> loadClass(className, loader))
                .flatMap(loaded -> loaded.toList().stream())
                .filter(classFilter)
                .forEach(result::add);
        return Set.copyOf(result);
    }

    public static Predicate<Class<?>> nonAbstractNonInterface() {
        return clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
    }

    private static Maybe<Class<?>> loadClass(String className, ClassLoader loader) {
        return CLASS_CACHE.computeIfAbsent(className, ignored ->
                Pathway.vtask((VTask<Class<?>>) () -> Class.forName(className, false, loader))
                        .asMaybeAll()
                        .unsafeRun());
    }

    private static boolean matchesBasePackages(String className, Set<String> basePackages) {
        if (basePackages == null || basePackages.isEmpty()) {
            return true;
        }
        for (String basePackage : basePackages) {
            if (className.equals(basePackage) || className.startsWith(basePackage + ".")) {
                return true;
            }
        }
        return false;
    }
}
