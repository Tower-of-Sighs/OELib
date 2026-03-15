package cc.sighs.oelib.fabric.util;

import cc.sighs.oelib.util.spi.IAnnotationScanner;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class FabricAnnotationScanner implements IAnnotationScanner {

    @Override
    public Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationType,
                                              Set<String> basePackages,
                                              Predicate<Class<?>> classFilter) {
        Set<Class<?>> result = new LinkedHashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        for (String basePackage : basePackages) {
            String path = basePackage.replace('.', '/');
            try {
                Enumeration<URL> resources = loader.getResources(path);
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    String protocol = url.getProtocol();
                    if ("file".equals(protocol)) {
                        scanDirectory(annotationType, new File(url.getPath()), basePackage, classFilter, loader, result);
                    } else if ("jar".equals(protocol)) {
                        scanJar(annotationType, url, path, basePackage, classFilter, loader, result);
                    }
                }
            } catch (IOException ignored) {}
        }
        return result;
    }

    private void scanDirectory(Class<? extends Annotation> ann, File dir, String pkg, Predicate<Class<?>> filter, ClassLoader l, Set<Class<?>> res) {
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDirectory(ann, f, pkg + "." + f.getName(), filter, l, res);
            } else if (f.getName().endsWith(".class")) {
                String name = pkg + "." + f.getName().substring(0, f.getName().length() - 6);
                maybeAdd(ann, name, filter, l, res);
            }
        }
    }

    private void scanJar(Class<? extends Annotation> ann, URL url, String path, String pkg, Predicate<Class<?>> filter, ClassLoader l, Set<Class<?>> res) {
        try {
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            try (JarFile jar = conn.getJarFile()) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(path) || !name.endsWith(".class") || entry.isDirectory()) continue;
                    String className = name.replace('/', '.').substring(0, name.length() - 6);
                    maybeAdd(ann, className, filter, l, res);
                }
            }
        } catch (IOException ignored) {}
    }

    private void maybeAdd(Class<? extends Annotation> ann, String name, Predicate<Class<?>> filter, ClassLoader l, Set<Class<?>> res) {
        try {
            Class<?> clazz = Class.forName(name, false, l);
            if (!filter.test(clazz)) return;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(ann)) {
                    res.add(clazz);
                    break;
                }
            }
        } catch (Throwable ignored) {}
    }
}
