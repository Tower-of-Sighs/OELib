package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigAffine;
import cc.sighs.oelib.config.optics.ConfigLens;
import cc.sighs.oelib.config.util.RecordLensClassGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Factory for building {@link ConfigLens} and {@link ConfigAffine} instances
 * that target components of Java {@code record} classes.
 *
 * <p>Lenses are created lazily and cached per (record class, component name, lookup class)
 * tuple. The backing implementation generates a hidden class via the Class-File API
 * at lookup time to avoid reflection overhead on every access.
 *
 * <p>Affines (optional and subtype views) are derived from lenses through
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
        String component = componentName(getter);
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
     * Returns an affine over the present value of an {@link Optional}-typed
     * lens target.
     *
     * <p>The returned affine focuses a value if and only if the target
     * optional is present.
     *
     * @param  lens the lens targeting an {@code Optional} field
     * @param  <S> the record type
     * @param  <A> the optional element type
     * @return an affine over the present optional element
     */
    public static <S, A> ConfigAffine<S, A> optional(ConfigLens<S, Optional<A>> lens) {
        Objects.requireNonNull(lens);
        return new ConfigAffine<>(
                lens.path(),
                lens::view,
                (value, source) -> lens.set(source, Optional.ofNullable(value))
        );
    }

    /**
     * Returns an affine over a lens target when the runtime value is an
     * instance of the given subtype.
     *
     * <p>The returned affine focuses a value if and only if the target value
     * is an instance of {@code subtypeClass}.
     *
     * @param  lens the lens targeting the base type
     * @param  subtypeClass the subtype required for a match
     * @param  <S> the record type
     * @param  <A> the base type
     * @param  <X> the subtype
     * @return an affine over values assignable to {@code subtypeClass}
     */
    public static <S, A, X extends A> ConfigAffine<S, X> subtype(ConfigLens<S, A> lens, Class<X> subtypeClass) {
        Objects.requireNonNull(lens);
        Objects.requireNonNull(subtypeClass);
        return new ConfigAffine<>(
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
        try {
            RecordComponent[] components = recordClass.getRecordComponents();
            int index = -1;
            for (int i = 0; i < components.length; i++) {
                if (components[i].getName().equals(componentName)) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                throw new IllegalArgumentException("Unknown record component '" + componentName + "' in " + recordClass.getName());
            }

            var accessors = RecordLensClassGenerator.generate(recordClass, components, index, lookup);

            Function<S, A> viewFn = source -> {
                try {
                    return (A) accessors.view().invokeExact((Object) source);
                } catch (Throwable t) {
                    throw new IllegalStateException("Failed to view record component " + componentName, t);
                }
            };
            BiFunction<A, S, S> setFn = (value, source) -> {
                try {
                    return (S) accessors.update().invokeExact((Object) value, (Object) source);
                } catch (Throwable t) {
                    throw new IllegalStateException("Failed to update record component " + componentName, t);
                }
            };
            return new ConfigLens<>(componentName, viewFn, setFn, ConfigLens.rootPlan(lookup, recordClass, components[index].getType(), componentName));
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to create record lens for " + recordClass.getName() + "#" + componentName, t);
        }
    }

    private static ConfigLens<?, ?> createPathLens(MethodHandles.Lookup lookup, PathLensKey key) {
        try {
            String[] parts = key.path().split("\\.");
            var accessors = RecordLensClassGenerator.generatePath(key.rootClass(), parts, lookup);
            Function<Object, Object> viewFn = source -> {
                try {
                    return accessors.view().invokeExact(source);
                } catch (Throwable t) {
                    throw new IllegalStateException("Failed to view record path " + key.path(), t);
                }
            };
            BiFunction<Object, Object, Object> setFn = (value, source) -> {
                try {
                    return accessors.update().invokeExact(value, source);
                } catch (Throwable t) {
                    throw new IllegalStateException("Failed to update record path " + key.path(), t);
                }
            };
            return new ConfigLens<>(key.path(), viewFn, setFn,
                    new ConfigLens.RecordLensPlan(lookup, key.rootClass(), key.leafClass(), List.of(parts)));
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to create path lens for " + key, t);
        }
    }

    static String componentName(Serializable getter) {
        try {
            return serializedLambda(getter).getImplMethodName();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to inspect getter lambda; use a record accessor method reference", e);
        }
    }

    static Class<?> componentType(Class<?> recordClass, String componentName) {
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(componentName);
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("Only record types are supported: " + recordClass.getName());
        }
        for (RecordComponent component : recordClass.getRecordComponents()) {
            if (component.getName().equals(componentName)) {
                return component.getType();
            }
        }
        throw new IllegalArgumentException("Unknown record component '" + componentName + "' in " + recordClass.getName());
    }

    static Class<?> optionalElementType(Serializable getter) {
        return genericReturnTypeArgument(getter, Optional.class, 0);
    }

    static Class<?> listElementType(Serializable getter) {
        return genericReturnTypeArgument(getter, List.class, 0);
    }

    static Class<?> mapKeyType(Serializable getter) {
        return genericReturnTypeArgument(getter, Map.class, 0);
    }

    static Class<?> mapValueType(Serializable getter) {
        return genericReturnTypeArgument(getter, Map.class, 1);
    }

    private static Class<?> genericReturnTypeArgument(Serializable getter, Class<?> rawType, int index) {
        try {
            SerializedLambda lambda = serializedLambda(getter);
            Class<?> implClass = Class.forName(lambda.getImplClass().replace('/', '.'));
            Method method = implClass.getDeclaredMethod(lambda.getImplMethodName());
            Type returnType = method.getGenericReturnType();
            if (returnType instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() == rawType) {
                Type argument = parameterizedType.getActualTypeArguments()[index];
                if (argument instanceof Class<?> clazz) {
                    return clazz;
                }
            }
            throw new IllegalArgumentException(
                    "Getter return type is not " + rawType.getSimpleName() + " with a reifiable type argument: " + lambda.getImplMethodName()
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to inspect getter lambda; use a record accessor method reference", e);
        }
    }

    private static SerializedLambda serializedLambda(Serializable getter) throws ReflectiveOperationException {
        var method = getter.getClass().getDeclaredMethod("writeReplace");
        method.setAccessible(true);
        return (SerializedLambda) method.invoke(getter);
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
