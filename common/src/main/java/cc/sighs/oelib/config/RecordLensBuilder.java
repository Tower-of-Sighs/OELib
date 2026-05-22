package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigLens;
import cc.sighs.oelib.config.optics.ConfigPrism;
import cc.sighs.oelib.config.util.RecordLensClassGenerator;
import com.mojang.datafixers.optics.Lens;
import com.mojang.datafixers.optics.Optics;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Factory for building {@link ConfigLens} and {@link ConfigPrism} instances
 * that target components of Java {@code record} classes.
 *
 * <p>Lenses are created lazily and cached per (record class, component name, lookup class)
 * tuple. The backing implementation generates a hidden class via the Class-File API
 * at lookup time to avoid reflection overhead on every access.
 *
 * <p>Prisms (optional and subtype views) are derived from lenses through
 * {@link #optional(ConfigLens)} and {@link #subtype(ConfigLens, Class)}.
 *
 * <p>Only record classes are supported. Passing a non-record class to any
 * {@code lens} method throws {@link IllegalArgumentException}.
 */
public final class RecordLensBuilder {
    private static final MethodHandles.Lookup INTERNAL_LOOKUP = MethodHandles.lookup();
    private static final ConcurrentMap<LensKey, ConfigLens<?, ?>> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<PathLensKey, ConfigLens<?, ?>> PATH_CACHE = new ConcurrentHashMap<>();

    private RecordLensBuilder() {
    }

    /**
     * Resolves a lens for the given record component accessor using the
     * default lookup.
     *
     * <p>The getter must be a serializable method reference to a record
     * component accessor (for example {@code MyRecord::value}). The component
     * name is extracted by serializing the lambda.
     *
     * @param recordClass the record class
     * @param getter      a serializable method reference to a record component
     * @param <S>         the record type
     * @param <A>         the component type
     * @return a lens targeting that component
     * @throws IllegalArgumentException if {@code recordClass} is not a record
     * @throws NullPointerException     if {@code recordClass} or {@code getter} is {@code null}
     */
    public static <S, A> ConfigLens<S, A> lens(Class<S> recordClass, LensGetter<S, A> getter) {
        return lens(INTERNAL_LOOKUP, recordClass, getter);
    }

    /**
     * Resolves a lens for the given record component accessor using an
     * explicit lookup.
     *
     * @param lookup      the lookup for access control
     * @param recordClass the record class
     * @param getter      a serializable method reference to a record component
     * @param <S>         the record type
     * @param <A>         the component type
     * @return a lens targeting that component
     * @throws IllegalArgumentException if {@code recordClass} is not a record
     * @throws NullPointerException     if any argument is {@code null}
     */
    public static <S, A> ConfigLens<S, A> lens(MethodHandles.Lookup lookup, Class<S> recordClass, LensGetter<S, A> getter) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(getter);
        String component = extractComponentName(getter);
        return lens(lookup, recordClass, component);
    }

    /**
     * Resolves a lens by component name using the default lookup.
     *
     * @param recordClass   the record class
     * @param componentName the name of the record component
     * @param <S>           the record type
     * @param <A>           the component type
     * @return a lens targeting that component
     * @throws IllegalArgumentException if the component does not exist
     * @throws NullPointerException     if any argument is {@code null}
     */
    public static <S, A> ConfigLens<S, A> lens(Class<S> recordClass, String componentName) {
        return lens(INTERNAL_LOOKUP, recordClass, componentName);
    }

    /**
     * Resolves a lens by component name using an explicit lookup.
     *
     * @param lookup        the lookup for access control
     * @param recordClass   the record class
     * @param componentName the name of the record component
     * @param <S>           the record type
     * @param <A>           the component type
     * @return a lens targeting that component
     * @throws IllegalArgumentException if {@code recordClass} is not a record or the
     *                                  component does not exist
     * @throws NullPointerException     if any argument is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <S, A> ConfigLens<S, A> lens(MethodHandles.Lookup lookup, Class<S> recordClass, String componentName) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(componentName);
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("Only record types are supported: " + recordClass.getName());
        }
        LensKey key = new LensKey(recordClass, componentName, lookup.lookupClass());
        return (ConfigLens<S, A>) CACHE.computeIfAbsent(key, k -> createLens(lookup, k.recordClass(), k.componentName()));
    }

    /**
     * Resolves a lens for a nested path of record components using the
     * default lookup.
     *
     * @param rootClass the root record class
     * @param path      the dotted path of component names
     * @param leafClass the type of the leaf component
     * @param <S>       the root record type
     * @param <A>       the leaf component type
     * @return a lens traversing the given path
     */
    public static <S, A> ConfigLens<S, A> lensByPath(Class<?> rootClass, List<String> path, Class<?> leafClass) {
        return lensByPath(INTERNAL_LOOKUP, rootClass, path, leafClass);
    }

    /**
     * Resolves a lens for a nested path of record components using an
     * explicit lookup.
     *
     * @param lookup    the lookup for access control
     * @param rootClass the root record class
     * @param path      the dotted path of component names
     * @param leafClass the type of the leaf component
     * @param <S>       the root record type
     * @param <A>       the leaf component type
     * @return a lens traversing the given path
     * @throws IllegalArgumentException if any intermediate type is not a record
     */
    @SuppressWarnings("unchecked")
    public static <S, A> ConfigLens<S, A> lensByPath(MethodHandles.Lookup lookup, Class<?> rootClass, List<String> path, Class<?> leafClass) {
        Objects.requireNonNull(lookup);
        PathLensKey key = new PathLensKey(rootClass, String.join(".", path), leafClass, lookup.lookupClass());
        return (ConfigLens<S, A>) PATH_CACHE.computeIfAbsent(key, k -> createPathLens(lookup, k));
    }

    /**
     * Wraps a lens over {@link Optional}{@code <A>} as a {@link ConfigPrism}
     * that only matches when the optional is present.
     *
     * @param lens a lens targeting an {@code Optional<A>} field
     * @param <S>  the record type
     * @param <A>  the inner optional type
     * @return a prism that views and updates the inner value when present
     * @throws NullPointerException if {@code lens} is {@code null}
     */
    public static <S, A> ConfigPrism<S, A> optional(ConfigLens<S, Optional<A>> lens) {
        Objects.requireNonNull(lens);
        return new ConfigPrism<>(
                lens.path(),
                lens::view,
                (value, source) -> lens.set(source, Optional.ofNullable(value))
        );
    }

    /**
     * Wraps a lens over a base type {@code A} as a {@link ConfigPrism} that
     * only matches when the runtime value is an instance of {@code subtypeClass}.
     *
     * @param lens         the lens targeting the base type
     * @param subtypeClass the expected subtype
     * @param <S>          the record type
     * @param <A>          the base type
     * @param <X>          the subtype
     * @return a prism that views and updates only when the value is of the subtype
     * @throws NullPointerException if any argument is {@code null}
     */
    public static <S, A, X extends A> ConfigPrism<S, X> subtype(ConfigLens<S, A> lens, Class<X> subtypeClass) {
        Objects.requireNonNull(lens);
        Objects.requireNonNull(subtypeClass);
        return new ConfigPrism<>(
                lens.path(),
                source -> {
                    A value = lens.view(source);
                    if (subtypeClass.isInstance(value)) {
                        return Optional.of(subtypeClass.cast(value));
                    }
                    return Optional.empty();
                },
                (value, source) -> lens.set(source, value)
        );
    }

    @SuppressWarnings("unchecked")
    private static <S, A> ConfigLens<S, A> createLens(MethodHandles.Lookup lookup, Class<?> recordClass, String componentName) {
        RecordComponent[] components = recordClass.getRecordComponents();
        int index = findComponentIndex(components, componentName, recordClass);
        try {
            var accessors = RecordLensClassGenerator.generate(recordClass, components, index, lookup);

            Lens<S, S, A, A> lens = Optics.lens(
                    source -> {
                        try {
                            return (A) accessors.view().invokeExact((Object) source);
                        } catch (Throwable t) {
                            throw new IllegalStateException("Failed to view record component " + componentName, t);
                        }
                    },
                    (value, source) -> {
                        try {
                            return (S) accessors.update().invokeExact((Object) value, (Object) source);
                        } catch (Throwable t) {
                            throw new IllegalStateException("Failed to update record component " + componentName, t);
                        }
                    }
            );
            return new ConfigLens<>(componentName, lens, ConfigLens.rootPlan(lookup, recordClass, components[index].getType(), componentName));
        } catch (RuntimeException e) {
            if (isModuleAccessFailure(e)) {
                return createReflectiveLens(recordClass, components, index, componentName);
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private static <S, A> ConfigLens<S, A> createReflectiveLens(Class<?> recordClass, RecordComponent[] components, int index, String componentName) {
        RecordComponent targetComp = components[index];
        Method accessor = targetComp.getAccessor();
        accessor.setAccessible(true);

        Class<?>[] paramTypes = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
        }
        Constructor<?> ctor;
        try {
            ctor = recordClass.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot find canonical constructor for " + recordClass.getName(), e);
        }

        Method[] componentAccessors = new Method[components.length];
        for (int i = 0; i < components.length; i++) {
            componentAccessors[i] = components[i].getAccessor();
            componentAccessors[i].setAccessible(true);
        }

        Lens<S, S, A, A> lens = Optics.lens(
                source -> {
                    try {
                        return (A) accessor.invoke(source);
                    } catch (Throwable t) {
                        throw new IllegalStateException("Failed to reflectively view " + componentName, t);
                    }
                },
                (value, source) -> {
                    try {
                        Object[] args = new Object[components.length];
                        for (int i = 0; i < components.length; i++) {
                            if (i == index) {
                                args[i] = value;
                            } else {
                                args[i] = componentAccessors[i].invoke(source);
                            }
                        }
                        return (S) ctor.newInstance(args);
                    } catch (Throwable t) {
                        throw new IllegalStateException("Failed to reflectively update " + componentName, t);
                    }
                }
        );
        return new ConfigLens<>(componentName, lens);
    }

    private static int findComponentIndex(RecordComponent[] components, String componentName, Class<?> recordClass) {
        for (int i = 0; i < components.length; i++) {
            if (components[i].getName().equals(componentName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown record component '" + componentName + "' in " + recordClass.getName());
    }

    /**
     * Returns {@code true} if the given exception was caused by a module-access
     * failure from {@link MethodHandles#privateLookupIn} or
     * {@link MethodHandles.Lookup#defineHiddenClass}.
     */
    private static boolean isModuleAccessFailure(Throwable t) {
        Throwable cause = t;
        while (cause != null) {
            if (cause instanceof IllegalAccessException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static ConfigLens<?, ?> createPathLens(MethodHandles.Lookup lookup, PathLensKey key) {
        try {
            String[] parts = key.path().split("\\.");
            var accessors = RecordLensClassGenerator.generatePath(key.rootClass(), parts, lookup);
            Lens<Object, Object, Object, Object> lens = Optics.lens(
                    source -> {
                        try {
                            return accessors.view().invokeExact(source);
                        } catch (Throwable t) {
                            throw new IllegalStateException("Failed to view record path " + key.path(), t);
                        }
                    },
                    (value, source) -> {
                        try {
                            return accessors.update().invokeExact(value, source);
                        } catch (Throwable t) {
                            throw new IllegalStateException("Failed to update record path " + key.path(), t);
                        }
                    }
            );
            return new ConfigLens<>(key.path(), lens,
                    new ConfigLens.RecordLensPlan(lookup, key.rootClass(), key.leafClass(), List.of(parts)));
        } catch (RuntimeException e) {
            if (isModuleAccessFailure(e)) {
                return createReflectivePathLens(key);
            }
            throw e;
        }
    }

    private static ConfigLens<?, ?> createReflectivePathLens(PathLensKey key) {
        String[] parts = key.path().split("\\.");
        Lens<Object, Object, Object, Object> lens = Optics.lens(
                source -> {
                    try {
                        Object current = source;
                        Class<?> currentClass = key.rootClass();
                        for (String part : parts) {
                            RecordComponent[] comps = currentClass.getRecordComponents();
                            RecordComponent comp = null;
                            for (RecordComponent c : comps) {
                                if (c.getName().equals(part)) {
                                    comp = c;
                                    break;
                                }
                            }
                            if (comp == null) {
                                throw new IllegalArgumentException("Unknown component '" + part + "' in " + currentClass.getName());
                            }
                            Method m = comp.getAccessor();
                            m.setAccessible(true);
                            Object next = m.invoke(current);
                            currentClass = comp.getType();
                            current = next;
                        }
                        return current;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Throwable t) {
                        throw new IllegalStateException("Failed to reflectively view path " + key.path(), t);
                    }
                },
                (value, source) -> {
                    try {
                        return rebuildPath(source, key.rootClass(), parts, parts.length - 1, value);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Throwable t) {
                        throw new IllegalStateException("Failed to reflectively update path " + key.path(), t);
                    }
                }
        );
        return new ConfigLens<>(key.path(), lens);
    }

    private static Object rebuildPath(Object source, Class<?> ownerClass, String[] parts, int depth, Object newValue) throws Throwable {
        RecordComponent[] components = ownerClass.getRecordComponents();
        Method[] accessors = new Method[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            accessors[i] = components[i].getAccessor();
            accessors[i].setAccessible(true);
            paramTypes[i] = components[i].getType();
        }
        Constructor<?> ctor = ownerClass.getDeclaredConstructor(paramTypes);
        ctor.setAccessible(true);

        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            if (components[i].getName().equals(parts[depth])) {
                if (depth == parts.length - 1) {
                    args[i] = newValue;
                } else {
                    Object nested = accessors[i].invoke(source);
                    args[i] = rebuildPath(nested, components[i].getType(), parts, depth + 1, newValue);
                }
            } else {
                args[i] = accessors[i].invoke(source);
            }
        }
        return ctor.newInstance(args);
    }

    private static String extractComponentName(Serializable getter) {
        try {
            var method = getter.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) method.invoke(getter);
            return lambda.getImplMethodName();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to inspect getter lambda; use a record accessor method reference", e);
        }
    }

    /**
     * A serializable function type used to capture record component accessor
     * method references for lens resolution.
     *
     * @param <S> the record type
     * @param <R> the component type
     */
    @FunctionalInterface
    public interface LensGetter<S, R> extends Function<S, R>, Serializable {
    }

    private record LensKey(Class<?> recordClass, String componentName, Class<?> lookupClass) {
        private LensKey {
            Objects.requireNonNull(recordClass);
            Objects.requireNonNull(componentName);
            Objects.requireNonNull(lookupClass);
        }

        @Override
        public @NotNull String toString() {
            return recordClass.getName() + "#" + componentName + "@" + lookupClass.getName() + Arrays.toString(recordClass.getRecordComponents());
        }
    }

    private record PathLensKey(Class<?> rootClass, String path, Class<?> leafClass, Class<?> lookupClass) {
        private PathLensKey {
            Objects.requireNonNull(rootClass);
            Objects.requireNonNull(path);
            Objects.requireNonNull(leafClass);
            Objects.requireNonNull(lookupClass);
        }
    }
}
