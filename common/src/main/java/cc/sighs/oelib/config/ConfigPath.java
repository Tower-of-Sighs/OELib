package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigAffine;
import cc.sighs.oelib.config.optics.ConfigLens;
import com.mojang.datafixers.util.Either;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Selects values within a configuration value.
 *
 * <p>Each instance focuses values with one of three cardinalities: exactly
 * one value, zero or one value, or zero or more values. Instances are created
 * from {@link ConfigSchema.Definition} and may be extended to nested
 * components without exposing the internal optic implementation types.
 *
 * @param <S> the root configuration type
 * @param <A> the focused value type
 *
 * @see ConfigSchema.Definition#path(RecordLensBuilder.LensGetter)
 * @see ConfigSchema.Definition#pathOptional(RecordLensBuilder.LensGetter)
 * @see ConfigSchema.Definition#pathEach(RecordLensBuilder.LensGetter)
 */
public sealed abstract class ConfigPath<S, A> permits ConfigPath.One, ConfigPath.Maybe, ConfigPath.Many {
    private final String path;

    private ConfigPath(String path) {
        this.path = Objects.requireNonNull(path);
    }

    /**
     * Returns the dotted path describing this selection.
     *
     * @return the dotted path
     */
    public final String path() {
        return path;
    }

    static <S, A> One<S, A> one(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            RecordLensBuilder.LensGetter<S, A> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        String component = RecordLensBuilder.componentName(getter);
        @SuppressWarnings("unchecked")
        Class<A> focusClass = (Class<A>) RecordLensBuilder.componentType(rootClass, component);
        return new One<>(lookup, rootClass, focusClass, RecordLensBuilder.lens(lookup, rootClass, getter));
    }

    static <S, A> Maybe<S, A> optional(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            RecordLensBuilder.LensGetter<S, Optional<A>> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        @SuppressWarnings("unchecked")
        Class<A> focusClass = (Class<A>) RecordLensBuilder.optionalElementType(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Maybe.writable(lookup, rootClass, focusClass, RecordLensBuilder.optional(lens));
    }

    static <S, A, X extends A> Maybe<S, X> subtype(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            RecordLensBuilder.LensGetter<S, A> getter,
            Class<X> subtypeClass
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(subtypeClass);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Maybe.writable(lookup, rootClass, subtypeClass, RecordLensBuilder.subtype(lens, subtypeClass));
    }

    static <S, E> Many<S, E> each(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            RecordLensBuilder.LensGetter<S, List<E>> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        @SuppressWarnings("unchecked")
        Class<E> focusClass = (Class<E>) RecordLensBuilder.listElementType(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Many.writable(
                lookup,
                rootClass,
                focusClass,
                lens.path(),
                source -> snapshot(lens.view(source)),
                (source, updater) -> lens.update(source, list -> mapList(list, updater))
        );
    }

    static <S, K, V> Many<S, V> values(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            RecordLensBuilder.LensGetter<S, Map<K, V>> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        @SuppressWarnings("unchecked")
        Class<V> focusClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Many.writable(
                lookup,
                rootClass,
                focusClass,
                lens.path(),
                source -> snapshot(new ArrayList<>(lens.view(source).values())),
                (source, updater) -> lens.update(source, map -> mapValues(map, updater))
        );
    }

    static <S, K, V> Many<S, K> keys(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            RecordLensBuilder.LensGetter<S, Map<K, V>> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        @SuppressWarnings("unchecked")
        Class<K> focusClass = (Class<K>) RecordLensBuilder.mapKeyType(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Many.readOnly(
                lookup,
                rootClass,
                focusClass,
                lens.path(),
                source -> snapshot(new ArrayList<>(lens.view(source).keySet()))
        );
    }

    static <S, K, V> Maybe<S, V> value(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            RecordLensBuilder.LensGetter<S, Map<K, V>> getter,
            K key
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(key);
        @SuppressWarnings("unchecked")
        Class<V> focusClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return new Maybe<>(
                lookup,
                rootClass,
                focusClass,
                lens.path() + "[" + key + "]",
                source -> getMapValue(lens.view(source), key),
                (value, source) -> lens.update(source, map -> replaceMapValue(map, key, value))
        );
    }

    private static <E> List<E> snapshot(List<E> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static <E> List<E> mapList(List<E> values, UnaryOperator<E> updater) {
        List<E> result = new ArrayList<>(values.size());
        for (E value : values) {
            result.add(updater.apply(value));
        }
        return result;
    }

    private static <K, V> Map<K, V> mapValues(Map<K, V> values, UnaryOperator<V> updater) {
        Map<K, V> result = new LinkedHashMap<>(Math.max(16, values.size()));
        for (Map.Entry<K, V> entry : values.entrySet()) {
            result.put(entry.getKey(), updater.apply(entry.getValue()));
        }
        return result;
    }

    private static <K, V> Optional<V> getMapValue(Map<K, V> values, K key) {
        return values.containsKey(key) ? Optional.ofNullable(values.get(key)) : Optional.empty();
    }

    private static <K, V> Map<K, V> replaceMapValue(Map<K, V> values, K key, V replacement) {
        if (!values.containsKey(key)) {
            return values;
        }
        Map<K, V> result = new LinkedHashMap<>(Math.max(16, values.size()));
        for (Map.Entry<K, V> entry : values.entrySet()) {
            result.put(entry.getKey(), Objects.equals(entry.getKey(), key) ? replacement : entry.getValue());
        }
        return result;
    }

    private static <S, A> BiFunction<A, S, S> requireWritable(BiFunction<A, S, S> setter, String path) {
        if (setter == null) {
            throw new UnsupportedOperationException("Path '" + path + "' is read-only");
        }
        return setter;
    }

    /**
     * Path selecting exactly one value.
     *
     * @param <S> the root configuration type
     * @param <A> the focused value type
     */
    public static final class One<S, A> extends ConfigPath<S, A> {
        private final MethodHandles.Lookup lookup;
        private final Class<S> rootClass;
        private final Class<A> focusClass;
        private final ConfigLens<S, A> lens;

        private One(MethodHandles.Lookup lookup, Class<S> rootClass, Class<A> focusClass, ConfigLens<S, A> lens) {
            super(lens.path());
            this.lookup = Objects.requireNonNull(lookup);
            this.rootClass = Objects.requireNonNull(rootClass);
            this.focusClass = Objects.requireNonNull(focusClass);
            this.lens = Objects.requireNonNull(lens);
        }

        /**
         * Returns the focused value from the given source.
         *
         * @param  source the source value
         * @return the focused value
         */
        public A view(S source) {
            return lens.view(source);
        }

        /**
         * Returns a source value with the focus replaced by the given value.
         *
         * @param  source the source value
         * @param  value the replacement value
         * @return a source value with the focus replaced
         */
        public S set(S source, A value) {
            return lens.set(source, value);
        }

        /**
         * Applies the given updater to the focused value.
         *
         * @param  source the source value
         * @param  updater the function that transforms the focused value
         * @return a source value with the focus updated
         */
        public S update(S source, UnaryOperator<A> updater) {
            Objects.requireNonNull(updater);
            return lens.update(source, updater);
        }

        /**
         * Returns a nested path for a child component of this focused value.
         *
         * @param  getter the child component accessor
         * @param  <B> the child component type
         * @return a path selecting the child component
         */
        public <B> One<S, B> then(RecordLensBuilder.LensGetter<A, B> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<B> childClass = (Class<B>) RecordLensBuilder.componentType(focusClass, RecordLensBuilder.componentName(getter));
            return new One<>(lookup, rootClass, childClass, lens.compose(child));
        }

        /**
         * Returns a zero-or-one path for an {@link Optional}-typed child
         * component of this focused value.
         *
         * @param  getter the optional child component accessor
         * @param  <B> the optional element type
         * @return a path selecting the present child value
         */
        public <B> Maybe<S, B> thenOptional(RecordLensBuilder.LensGetter<A, Optional<B>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<B> childClass = (Class<B>) RecordLensBuilder.optionalElementType(getter);
            return Maybe.writable(lookup, rootClass, childClass, RecordLensBuilder.optional(lens.compose(child)));
        }

        /**
         * Returns a zero-or-one path for a child component that must be an
         * instance of the given subtype.
         *
         * @param  getter the child component accessor
         * @param  subtypeClass the subtype required for a match
         * @param  <V> the base type
         * @param  <X> the subtype
         * @return a path selecting the child value when it is of the given subtype
         */
        public <V, X extends V> Maybe<S, X> thenSubtype(RecordLensBuilder.LensGetter<A, V> getter, Class<X> subtypeClass) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(subtypeClass);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            return Maybe.writable(lookup, rootClass, subtypeClass, RecordLensBuilder.subtype(lens.compose(child), subtypeClass));
        }

        /**
         * Returns a zero-or-one path for this focused value when it is an
         * instance of the given subtype.
         *
         * @param  subtypeClass the subtype required for a match
         * @param  <X> the subtype
         * @return a path selecting this value when it is of the given subtype
         */
        public <X extends A> Maybe<S, X> as(Class<X> subtypeClass) {
            Objects.requireNonNull(subtypeClass);
            return Maybe.writable(lookup, rootClass, subtypeClass, RecordLensBuilder.subtype(lens, subtypeClass));
        }

        /**
         * Returns a zero-or-more path for all elements of a list-valued child
         * component.
         *
         * @param  getter the list child component accessor
         * @param  <E> the list element type
         * @return a path selecting all child list elements
         */
        public <E> Many<S, E> thenEach(RecordLensBuilder.LensGetter<A, List<E>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<E> childClass = (Class<E>) RecordLensBuilder.listElementType(getter);
            return Many.writable(
                    lookup,
                    rootClass,
                    childClass,
                    lens.path() + "." + child.path(),
                    source -> snapshot(child.view(lens.view(source))),
                    (source, updater) -> lens.update(source, parent -> child.set(parent, mapList(child.view(parent), updater)))
            );
        }

        /**
         * Returns a zero-or-more path for all values of a map-valued child
         * component.
         *
         * @param  getter the map child component accessor
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting all child map values
         */
        public <K, V> Many<S, V> thenValues(RecordLensBuilder.LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            return Many.writable(
                    lookup,
                    rootClass,
                    childClass,
                    lens.path() + "." + child.path(),
                    source -> snapshot(new ArrayList<>(child.view(lens.view(source)).values())),
                    (source, updater) -> lens.update(source, parent -> child.set(parent, mapValues(child.view(parent), updater)))
            );
        }

        /**
         * Returns a query-only path for all keys of a map-valued child
         * component.
         *
         * @param  getter the map child component accessor
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting all child map keys
         */
        public <K, V> Many<S, K> thenKeys(RecordLensBuilder.LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<K> childClass = (Class<K>) RecordLensBuilder.mapKeyType(getter);
            return Many.readOnly(
                    lookup,
                    rootClass,
                    childClass,
                    lens.path() + "." + child.path(),
                    source -> snapshot(new ArrayList<>(child.view(lens.view(source)).keySet()))
            );
        }

        /**
         * Returns a path selecting the value stored at the given key in a
         * map-valued child component.
         *
         * @param  getter the map child component accessor
         * @param  key the map key to resolve
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting the map value stored at {@code key}
         */
        public <K, V> Maybe<S, V> thenValue(RecordLensBuilder.LensGetter<A, Map<K, V>> getter, K key) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(key);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            return new Maybe<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path() + "[" + key + "]",
                    source -> getMapValue(child.view(lens.view(source)), key),
                    (value, source) -> lens.update(source, parent -> child.set(parent, replaceMapValue(child.view(parent), key, value)))
            );
        }
    }

    /**
     * Path selecting zero or one value.
     *
     * @param <S> the root configuration type
     * @param <A> the focused value type
     */
    public static final class Maybe<S, A> extends ConfigPath<S, A> {
        private final MethodHandles.Lookup lookup;
        private final Class<S> rootClass;
        private final Class<A> focusClass;
        private final Function<S, Optional<A>> previewFn;
        private final BiFunction<A, S, S> setter;

        private Maybe(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Function<S, Optional<A>> previewFn,
                BiFunction<A, S, S> setter
        ) {
            super(path);
            this.lookup = Objects.requireNonNull(lookup);
            this.rootClass = Objects.requireNonNull(rootClass);
            this.focusClass = Objects.requireNonNull(focusClass);
            this.previewFn = Objects.requireNonNull(previewFn);
            this.setter = setter;
        }

        private static <S, A> Maybe<S, A> writable(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                ConfigAffine<S, A> affine
        ) {
            return new Maybe<>(lookup, rootClass, focusClass, affine.path(), affine::preview, (value, source) -> affine.set(source, value));
        }

        private static <S, A> Maybe<S, A> readOnly(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Function<S, Optional<A>> previewFn
        ) {
            return new Maybe<>(lookup, rootClass, focusClass, path, previewFn, null);
        }

        /**
         * Returns the focused value, if any.
         *
         * @param  source the source value
         * @return an {@link Optional} containing the focused value, or
         *         {@link Optional#empty()} if no value is focused
         */
        public Optional<A> preview(S source) {
            return previewFn.apply(source);
        }

        /**
         * Returns either the focused value or the original source when no value
         * is focused.
         *
         * @param  source the source value
         * @return {@code Either.right(value)} when a value is focused;
         *         {@code Either.left(source)} otherwise
         */
        public Either<S, A> match(S source) {
            return preview(source).<Either<S, A>>map(Either::right).orElseGet(() -> Either.left(source));
        }

        /**
         * Returns a source value with the focused value replaced.
         *
         * <p>If this path is read-only or no value is focused within
         * {@code source}, the result is unspecified.
         *
         * @param  source the source value
         * @param  value the replacement value
         * @return a source value with the focused value replaced
         */
        public S set(S source, A value) {
            return requireWritable(setter, path()).apply(value, source);
        }

        /**
         * Applies the given updater when a value is focused.
         *
         * @param  source the source value
         * @param  updater the function that transforms the focused value
         * @return a source value with the focused value updated, or
         *         {@code source} if no value is focused
         */
        public S updateIfPresent(S source, UnaryOperator<A> updater) {
            Objects.requireNonNull(updater);
            BiFunction<A, S, S> writable = requireWritable(setter, path());
            return preview(source)
                    .map(value -> writable.apply(updater.apply(value), source))
                    .orElse(source);
        }

        /**
         * Returns a nested zero-or-one path for a child component of the
         * focused value.
         *
         * @param  getter the child component accessor
         * @param  <B> the child component type
         * @return a path selecting the child component when this path focuses a value
         */
        public <B> Maybe<S, B> then(RecordLensBuilder.LensGetter<A, B> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<B> childClass = (Class<B>) RecordLensBuilder.componentType(focusClass, RecordLensBuilder.componentName(getter));
            return new Maybe<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> preview(source).map(child::view),
                    setter == null ? null : (value, source) -> updateIfPresent(source, parent -> child.set(parent, value))
            );
        }

        /**
         * Returns a nested zero-or-one path for an {@link Optional}-typed child
         * component of the focused value.
         *
         * @param  getter the optional child component accessor
         * @param  <B> the optional element type
         * @return a path selecting the present child value when this path focuses a value
         */
        public <B> Maybe<S, B> thenOptional(RecordLensBuilder.LensGetter<A, Optional<B>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<B> childClass = (Class<B>) RecordLensBuilder.optionalElementType(getter);
            return new Maybe<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> preview(source).flatMap(child::view),
                    setter == null ? null : (value, source) -> updateIfPresent(source, parent -> child.set(parent, Optional.ofNullable(value)))
            );
        }

        /**
         * Returns a nested zero-or-one path for a child component that must be
         * an instance of the given subtype.
         *
         * @param  getter the child component accessor
         * @param  subtypeClass the subtype required for a match
         * @param  <V> the base type
         * @param  <X> the subtype
         * @return a path selecting the child value when it is of the given subtype
         */
        public <V, X extends V> Maybe<S, X> thenSubtype(RecordLensBuilder.LensGetter<A, V> getter, Class<X> subtypeClass) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(subtypeClass);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            return new Maybe<>(
                    lookup,
                    rootClass,
                    subtypeClass,
                    path() + "." + child.path(),
                    source -> preview(source).flatMap(value -> {
                        V childValue = child.view(value);
                        return subtypeClass.isInstance(childValue)
                                ? Optional.of(subtypeClass.cast(childValue))
                                : Optional.empty();
                    }),
                    setter == null ? null : (value, source) -> updateIfPresent(source, parent -> child.set(parent, value))
            );
        }

        /**
         * Returns a zero-or-more path for all elements of a list-valued child
         * component.
         *
         * @param  getter the list child component accessor
         * @param  <E> the list element type
         * @return a path selecting all child list elements when this path focuses a value
         */
        public <E> Many<S, E> thenEach(RecordLensBuilder.LensGetter<A, List<E>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<E> childClass = (Class<E>) RecordLensBuilder.listElementType(getter);
            return new Many<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> preview(source)
                            .map(value -> snapshot(child.view(value)))
                            .orElseGet(List::of),
                    setter == null ? null : (source, updater) -> updateIfPresent(source, parent -> child.set(parent, mapList(child.view(parent), updater)))
            );
        }

        /**
         * Returns a zero-or-more path for all values of a map-valued child
         * component.
         *
         * @param  getter the map child component accessor
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting all child map values when this path focuses a value
         */
        public <K, V> Many<S, V> thenValues(RecordLensBuilder.LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            return new Many<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> preview(source)
                            .map(value -> snapshot(new ArrayList<>(child.view(value).values())))
                            .orElseGet(List::of),
                    setter == null ? null : (source, updater) -> updateIfPresent(source, parent -> child.set(parent, mapValues(child.view(parent), updater)))
            );
        }

        /**
         * Returns a query-only path for all keys of a map-valued child
         * component.
         *
         * @param  getter the map child component accessor
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting all child map keys when this path focuses a value
         */
        public <K, V> Many<S, K> thenKeys(RecordLensBuilder.LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<K> childClass = (Class<K>) RecordLensBuilder.mapKeyType(getter);
            return Many.readOnly(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> preview(source)
                            .map(value -> snapshot(new ArrayList<>(child.view(value).keySet())))
                            .orElseGet(List::of)
            );
        }

        /**
         * Returns a nested path selecting the value stored at the given key in
         * a map-valued child component.
         *
         * @param  getter the map child component accessor
         * @param  key the map key to resolve
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting the map value stored at {@code key} when
         *         this path focuses a value
         */
        public <K, V> Maybe<S, V> thenValue(RecordLensBuilder.LensGetter<A, Map<K, V>> getter, K key) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(key);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            return new Maybe<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path() + "[" + key + "]",
                    source -> preview(source).flatMap(value -> getMapValue(child.view(value), key)),
                    setter == null ? null : (value, source) -> updateIfPresent(
                            source,
                            parent -> child.set(parent, replaceMapValue(child.view(parent), key, value))
                    )
            );
        }
    }

    /**
     * Path selecting zero or more values.
     *
     * @param <S> the root configuration type
     * @param <A> the focused value type
     */
    public static final class Many<S, A> extends ConfigPath<S, A> {
        private final MethodHandles.Lookup lookup;
        private final Class<S> rootClass;
        private final Class<A> focusClass;
        private final Function<S, List<A>> extractFn;
        private final BiFunction<S, UnaryOperator<A>, S> updateFn;

        private Many(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Function<S, List<A>> extractFn,
                BiFunction<S, UnaryOperator<A>, S> updateFn
        ) {
            super(path);
            this.lookup = Objects.requireNonNull(lookup);
            this.rootClass = Objects.requireNonNull(rootClass);
            this.focusClass = Objects.requireNonNull(focusClass);
            this.extractFn = Objects.requireNonNull(extractFn);
            this.updateFn = updateFn;
        }

        private static <S, A> Many<S, A> writable(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Function<S, List<A>> extractFn,
                BiFunction<S, UnaryOperator<A>, S> updateFn
        ) {
            return new Many<>(lookup, rootClass, focusClass, path, extractFn, updateFn);
        }

        private static <S, A> Many<S, A> readOnly(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Function<S, List<A>> extractFn
        ) {
            return new Many<>(lookup, rootClass, focusClass, path, extractFn, null);
        }

        /**
         * Returns all focused values from the given source.
         *
         * @param  source the source value
         * @return an immutable list of focused values
         */
        public List<A> getAll(S source) {
            return snapshot(extractFn.apply(source));
        }

        /**
         * Returns the number of focused values.
         *
         * @param  source the source value
         * @return the number of focused values
         */
        public long count(S source) {
            return extractFn.apply(source).size();
        }

        /**
         * Tests whether any focused value satisfies the given predicate.
         *
         * @param  source the source value
         * @param  predicate the predicate to test
         * @return {@code true} if any focused value satisfies {@code predicate}
         */
        public boolean anyMatch(S source, Predicate<? super A> predicate) {
            Objects.requireNonNull(predicate);
            return extractFn.apply(source).stream().anyMatch(predicate);
        }

        /**
         * Tests whether all focused values satisfy the given predicate.
         *
         * @param  source the source value
         * @param  predicate the predicate to test
         * @return {@code true} if all focused values satisfy {@code predicate},
         *         or if no values are focused
         */
        public boolean allMatch(S source, Predicate<? super A> predicate) {
            Objects.requireNonNull(predicate);
            return extractFn.apply(source).stream().allMatch(predicate);
        }

        /**
         * Returns the first focused value that satisfies the given predicate.
         *
         * @param  source the source value
         * @param  predicate the predicate to test
         * @return an {@link Optional} containing the first matching focused
         *         value, or {@link Optional#empty()} if no focused value
         *         matches
         */
        public Optional<A> findFirst(S source, Predicate<? super A> predicate) {
            Objects.requireNonNull(predicate);
            for (A value : extractFn.apply(source)) {
                if (predicate.test(value)) {
                    return Optional.ofNullable(value);
                }
            }
            return Optional.empty();
        }

        /**
         * Applies the given updater to all focused values.
         *
         * @param  source the source value
         * @param  updater the function that transforms each focused value
         * @return a source value with all focused values updated
         */
        public S updateEach(S source, UnaryOperator<A> updater) {
            Objects.requireNonNull(updater);
            if (updateFn == null) {
                throw new UnsupportedOperationException("Path '" + path() + "' is read-only");
            }
            return updateFn.apply(source, updater);
        }

        /**
         * Returns a path that selects only focused values satisfying the given
         * predicate.
         *
         * @param  predicate the selection predicate
         * @return a filtered path
         */
        public Many<S, A> where(Predicate<? super A> predicate) {
            Objects.requireNonNull(predicate);
            return new Many<>(
                    lookup,
                    rootClass,
                    focusClass,
                    path(),
                    source -> extractFn.apply(source).stream().filter(predicate).toList(),
                    updateFn == null ? null : (source, updater) -> updateFn.apply(source, value -> predicate.test(value) ? updater.apply(value) : value)
            );
        }

        /**
         * Returns a zero-or-one path for the focused value at the given index.
         *
         * <p>If fewer than {@code index + 1} values are focused within a source,
         * the returned path does not focus a value for that source.
         *
         * @param  index the zero-based focused-value index
         * @return a path selecting the focused value at {@code index}
         */
        public Maybe<S, A> at(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0");
            }
            return new Maybe<>(
                    lookup,
                    rootClass,
                    focusClass,
                    path() + "[" + index + "]",
                    source -> {
                        List<A> values = extractFn.apply(source);
                        return index < values.size() ? Optional.ofNullable(values.get(index)) : Optional.empty();
                    },
                    updateFn == null ? null : (value, source) -> {
                        int[] current = {0};
                        return updateFn.apply(source, focused -> current[0]++ == index ? value : focused);
                    }
            );
        }

        /**
         * Returns a nested path for a child component of each focused value.
         *
         * @param  getter the child component accessor
         * @param  <B> the child component type
         * @return a path selecting each child component
         */
        public <B> Many<S, B> then(RecordLensBuilder.LensGetter<A, B> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<B> childClass = (Class<B>) RecordLensBuilder.componentType(focusClass, RecordLensBuilder.componentName(getter));
            return new Many<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> {
                        List<A> parents = extractFn.apply(source);
                        List<B> result = new ArrayList<>(parents.size());
                        for (A parent : parents) {
                            result.add(child.view(parent));
                        }
                        return result;
                    },
                    updateFn == null ? null : (source, updater) -> updateFn.apply(source, parent -> child.set(parent, updater.apply(child.view(parent))))
            );
        }

        /**
         * Returns a nested path for present values of an
         * {@link Optional}-typed child component of each focused value.
         *
         * @param  getter the optional child component accessor
         * @param  <B> the optional element type
         * @return a path selecting all present child values
         */
        public <B> Many<S, B> thenOptional(RecordLensBuilder.LensGetter<A, Optional<B>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<B> childClass = (Class<B>) RecordLensBuilder.optionalElementType(getter);
            return new Many<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> {
                        List<B> result = new ArrayList<>();
                        for (A parent : extractFn.apply(source)) {
                            child.view(parent).ifPresent(result::add);
                        }
                        return snapshot(result);
                    },
                    updateFn == null ? null : (source, updater) -> updateFn.apply(source, parent -> {
                        Optional<B> current = child.view(parent);
                        return current.isPresent()
                                ? child.set(parent, Optional.ofNullable(updater.apply(current.get())))
                                : parent;
                    })
            );
        }

        /**
         * Returns a nested path for child component values that are instances
         * of the given subtype.
         *
         * @param  getter the child component accessor
         * @param  subtypeClass the required subtype
         * @param  <V> the base type
         * @param  <X> the subtype
         * @return a path selecting child values assignable to {@code subtypeClass}
         */
        public <V, X extends V> Many<S, X> thenSubtype(RecordLensBuilder.LensGetter<A, V> getter, Class<X> subtypeClass) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(subtypeClass);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            return new Many<>(
                    lookup,
                    rootClass,
                    subtypeClass,
                    path() + "." + child.path(),
                    source -> extractFn.apply(source).stream()
                            .map(child::view)
                            .filter(subtypeClass::isInstance)
                            .map(subtypeClass::cast)
                            .toList(),
                    updateFn == null ? null : (source, updater) -> updateFn.apply(source, parent -> {
                        V value = child.view(parent);
                        return subtypeClass.isInstance(value)
                                ? child.set(parent, updater.apply(subtypeClass.cast(value)))
                                : parent;
                    })
            );
        }

        /**
         * Returns a nested path for all elements of a list-valued child
         * component of each focused value.
         *
         * @param  getter the list child component accessor
         * @param  <B> the list element type
         * @return a path selecting all child list elements
         */
        public <B> Many<S, B> thenEach(RecordLensBuilder.LensGetter<A, List<B>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<B> childClass = (Class<B>) RecordLensBuilder.listElementType(getter);
            return new Many<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> {
                        List<B> result = new ArrayList<>();
                        for (A parent : extractFn.apply(source)) {
                            result.addAll(child.view(parent));
                        }
                        return snapshot(result);
                    },
                    updateFn == null ? null : (source, updater) -> updateFn.apply(source, parent -> child.set(parent, mapList(child.view(parent), updater)))
            );
        }

        /**
         * Returns a nested path for all values of a map-valued child component
         * of each focused value.
         *
         * @param  getter the map child component accessor
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting all child map values
         */
        public <K, V> Many<S, V> thenValues(RecordLensBuilder.LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            return new Many<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> {
                        List<V> result = new ArrayList<>();
                        for (A parent : extractFn.apply(source)) {
                            result.addAll(child.view(parent).values());
                        }
                        return snapshot(result);
                    },
                    updateFn == null ? null : (source, updater) -> updateFn.apply(source, parent -> child.set(parent, mapValues(child.view(parent), updater)))
            );
        }

        /**
         * Returns a query-only path for all keys of a map-valued child
         * component of each focused value.
         *
         * @param  getter the map child component accessor
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting all child map keys
         */
        public <K, V> Many<S, K> thenKeys(RecordLensBuilder.LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<K> childClass = (Class<K>) RecordLensBuilder.mapKeyType(getter);
            return Many.readOnly(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path(),
                    source -> {
                        List<K> result = new ArrayList<>();
                        for (A parent : extractFn.apply(source)) {
                            result.addAll(child.view(parent).keySet());
                        }
                        return snapshot(result);
                    }
            );
        }

        /**
         * Returns a nested path selecting the value stored at the given key in
         * a map-valued child component of each focused value.
         *
         * @param  getter the map child component accessor
         * @param  key the map key to resolve
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting the map value stored at {@code key} from
         *         each focused value
         */
        public <K, V> Many<S, V> thenValue(RecordLensBuilder.LensGetter<A, Map<K, V>> getter, K key) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(key);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            @SuppressWarnings("unchecked")
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            return new Many<>(
                    lookup,
                    rootClass,
                    childClass,
                    path() + "." + child.path() + "[" + key + "]",
                    source -> {
                        List<V> result = new ArrayList<>();
                        for (A parent : extractFn.apply(source)) {
                            getMapValue(child.view(parent), key).ifPresent(result::add);
                        }
                        return snapshot(result);
                    },
                    updateFn == null ? null : (source, updater) -> updateFn.apply(source, parent -> {
                        Map<K, V> values = child.view(parent);
                        return values.containsKey(key)
                                ? child.set(parent, replaceMapValue(values, key, updater.apply(values.get(key))))
                                : parent;
                    })
            );
        }

        /**
         * Returns a filtered path selecting only focused values assignable to
         * the given subtype.
         *
         * @param  subtypeClass the required subtype
         * @param  <X> the subtype
         * @return a filtered path selecting only values of the given subtype
         */
        public <X extends A> Many<S, X> as(Class<X> subtypeClass) {
            Objects.requireNonNull(subtypeClass);
            return new Many<>(
                    lookup,
                    rootClass,
                    subtypeClass,
                    path(),
                    source -> extractFn.apply(source).stream()
                            .filter(subtypeClass::isInstance)
                            .map(subtypeClass::cast)
                            .toList(),
                    updateFn == null ? null : (source, updater) -> updateFn.apply(source, value -> subtypeClass.isInstance(value) ? updater.apply(subtypeClass.cast(value)) : value)
            );
        }
    }

}
