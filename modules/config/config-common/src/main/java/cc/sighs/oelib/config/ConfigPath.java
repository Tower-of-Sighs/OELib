package cc.sighs.oelib.config;

import com.flechazo.optics.*;
import com.flechazo.optics.generated.LensGetter;
import com.flechazo.optics.util.Affines;
import com.flechazo.optics.util.Prisms;
import com.mojang.datafixers.util.Either;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 * @see ConfigSchema.Definition#path(LensGetter)
 * @see ConfigSchema.Definition#pathOptional(LensGetter)
 * @see ConfigSchema.Definition#pathEach(LensGetter)
 */
@SuppressWarnings("unchecked")
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
            LensGetter<S, A> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        String component = RecordLensBuilder.componentName(getter);

        Class<A> focusClass = (Class<A>) RecordLensBuilder.componentType(rootClass, component);
        return new One<>(lookup, rootClass, focusClass, component, RecordLensBuilder.lens(lookup, rootClass, getter));
    }

    static <S, A> Maybe<S, A> optional(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            LensGetter<S, Optional<A>> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);

        Class<A> focusClass = (Class<A>) RecordLensBuilder.optionalElementType(getter);
        String component = RecordLensBuilder.componentName(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Maybe.writable(lookup, rootClass, focusClass, component, RecordLensBuilder.optional(lens));
    }

    static <S, A, X extends A> Maybe<S, X> subtype(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            LensGetter<S, A> getter,
            Class<X> subtypeClass
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(subtypeClass);
        String component = RecordLensBuilder.componentName(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Maybe.writable(lookup, rootClass, subtypeClass, component, RecordLensBuilder.subtype(lens, subtypeClass));
    }

    static <S, E> Many<S, E> each(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            LensGetter<S, List<E>> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);

        Class<E> focusClass = (Class<E>) RecordLensBuilder.listElementType(getter);
        String component = RecordLensBuilder.componentName(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Many.writable(lookup, rootClass, focusClass, component, lens.andThen(Each.listTraversal()));
    }

    static <S, K, V> Many<S, V> values(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            LensGetter<S, Map<K, V>> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);

        Class<V> focusClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
        String component = RecordLensBuilder.componentName(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Many.writable(lookup, rootClass, focusClass, component, lens.andThen(Traversal.mapValues()));
    }

    static <S, K, V> Many<S, K> keys(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            LensGetter<S, Map<K, V>> getter
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);

        Class<K> focusClass = (Class<K>) RecordLensBuilder.mapKeyType(getter);
        String component = RecordLensBuilder.componentName(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Many.readOnly(lookup, rootClass, focusClass, component, lens.andThen(Fold.mapKeys()));
    }

    static <S, K, V> Maybe<S, V> value(
            MethodHandles.Lookup lookup,
            Class<S> rootClass,
            LensGetter<S, Map<K, V>> getter,
            K key
    ) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(key);

        Class<V> focusClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
        String component = RecordLensBuilder.componentName(getter);
        var lens = RecordLensBuilder.lens(lookup, rootClass, getter);
        return Maybe.writable(lookup, rootClass, focusClass, component + "[" + key + "]", lens.andThen(Affine.mapValue(key)));
    }

    private static <S, A> Traversal<S, A> requireTraversal(Traversal<S, A> traversal, String path) {
        if (traversal == null) {
            throw new UnsupportedOperationException("Path '" + path + "' is read-only");
        }
        return traversal;
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
        private final Lens<S, A> lens;

        private One(MethodHandles.Lookup lookup, Class<S> rootClass, Class<A> focusClass, String path, Lens<S, A> lens) {
            super(path);
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
            return lens.get(source);
        }

        /**
         * Returns a source value with the focus replaced by the given value.
         *
         * @param  source the source value
         * @param  value the replacement value
         * @return a source value with the focus replaced
         */
        public S set(S source, A value) {
            return lens.set(value, source);
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
            return lens.modify(updater, source);
        }

        /**
         * Returns a nested path for a child component of this focused value.
         *
         * @param  getter the child component accessor
         * @param  <B> the child component type
         * @return a path selecting the child component
         */
        public <B> One<S, B> then(LensGetter<A, B> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<B> childClass = (Class<B>) RecordLensBuilder.componentType(focusClass, RecordLensBuilder.componentName(getter));
            String component = RecordLensBuilder.componentName(getter);
            return new One<>(lookup, rootClass, childClass, path() + "." + component, lens.andThen(child));
        }

        /**
         * Returns a zero-or-one path for an {@link Optional}-typed child
         * component of this focused value.
         *
         * @param  getter the optional child component accessor
         * @param  <B> the optional element type
         * @return a path selecting the present child value
         */
        public <B> Maybe<S, B> thenOptional(LensGetter<A, Optional<B>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<B> childClass = (Class<B>) RecordLensBuilder.optionalElementType(getter);
            String component = RecordLensBuilder.componentName(getter);
            return Maybe.writable(lookup, rootClass, childClass, path() + "." + component, lens.andThen(RecordLensBuilder.optional(child)));
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
        public <V, X extends V> Maybe<S, X> thenSubtype(LensGetter<A, V> getter, Class<X> subtypeClass) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(subtypeClass);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            String component = RecordLensBuilder.componentName(getter);
            return Maybe.writable(lookup, rootClass, subtypeClass, path() + "." + component, lens.andThen(RecordLensBuilder.subtype(child, subtypeClass)));
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
            return Maybe.writable(lookup, rootClass, subtypeClass, path(), RecordLensBuilder.subtype(lens, subtypeClass));
        }

        /**
         * Returns a zero-or-more path for all elements of a list-valued child
         * component.
         *
         * @param  getter the list child component accessor
         * @param  <E> the list element type
         * @return a path selecting all child list elements
         */
        public <E> Many<S, E> thenEach(LensGetter<A, List<E>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<E> childClass = (Class<E>) RecordLensBuilder.listElementType(getter);
            String component = RecordLensBuilder.componentName(getter);
            return Many.writable(lookup, rootClass, childClass, path() + "." + component, lens.andThen(child).andThen(Each.listTraversal()));
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
        public <K, V> Many<S, V> thenValues(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            String component = RecordLensBuilder.componentName(getter);
            return Many.writable(lookup, rootClass, childClass, path() + "." + component, lens.andThen(child).andThen(Traversal.mapValues()));
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
        public <K, V> Many<S, K> thenKeys(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<K> childClass = (Class<K>) RecordLensBuilder.mapKeyType(getter);
            String component = RecordLensBuilder.componentName(getter);
            return Many.readOnly(lookup, rootClass, childClass, path() + "." + component, lens.andThen(child).andThen(Fold.mapKeys()));
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
        public <K, V> Maybe<S, V> thenValue(LensGetter<A, Map<K, V>> getter, K key) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(key);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            String component = RecordLensBuilder.componentName(getter);
            return Maybe.writable(lookup, rootClass, childClass, path() + "." + component + "[" + key + "]", lens.andThen(child).andThen(Affine.mapValue(key)));
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
        private final Affine<S, A> affine;
        private final boolean writable;

        private Maybe(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Affine<S, A> affine,
                boolean writable
        ) {
            super(path);
            this.lookup = Objects.requireNonNull(lookup);
            this.rootClass = Objects.requireNonNull(rootClass);
            this.focusClass = Objects.requireNonNull(focusClass);
            this.affine = Objects.requireNonNull(affine);
            this.writable = writable;
        }

        private static <S, A> Maybe<S, A> writable(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Affine<S, A> affine
        ) {
            return new Maybe<>(lookup, rootClass, focusClass, path, affine, true);
        }

        private static <S, A> Maybe<S, A> readOnly(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Affine<S, A> affine
        ) {
            return new Maybe<>(lookup, rootClass, focusClass, path, affine, false);
        }

        /**
         * Returns the focused value, if any.
         *
         * @param  source the source value
         * @return an {@link Optional} containing the focused value, or
         *         {@link Optional#empty()} if no value is focused
         */
        public Optional<A> preview(S source) {
            return Affines.previewOptional(affine, source);
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
            return affine.getMaybe(source).isDefined()
                    ? Either.right(affine.getMaybe(source).get())
                    : Either.left(source);
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
            return affine.set(value, source);
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
            return affine.modify(updater, source);
        }

        /**
         * Returns a path that selects this value only when it satisfies the
         * given predicate.
         *
         * @param  predicate the selection predicate
         * @return a filtered path
         */
        public Maybe<S, A> where(Predicate<? super A> predicate) {
            Objects.requireNonNull(predicate);
            Affine<S, A> filtered = affine.filtered(predicate);
            return writable
                    ? Maybe.writable(lookup, rootClass, focusClass, path(), filtered)
                    : Maybe.readOnly(lookup, rootClass, focusClass, path(), filtered);
        }

        /**
         * Returns a nested zero-or-one path for a child component of the
         * focused value.
         *
         * @param  getter the child component accessor
         * @param  <B> the child component type
         * @return a path selecting the child component when this path focuses a value
         */
        public <B> Maybe<S, B> then(LensGetter<A, B> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<B> childClass = (Class<B>) RecordLensBuilder.componentType(focusClass, RecordLensBuilder.componentName(getter));
            return writable
                    ? Maybe.writable(lookup, rootClass, childClass, path() + "." + RecordLensBuilder.componentName(getter), affine.andThen(child))
                    : Maybe.readOnly(lookup, rootClass, childClass, path() + "." + RecordLensBuilder.componentName(getter), affine.andThen(child));
        }

        /**
         * Returns a nested zero-or-one path for an {@link Optional}-typed child
         * component of the focused value.
         *
         * @param  getter the optional child component accessor
         * @param  <B> the optional element type
         * @return a path selecting the present child value when this path focuses a value
         */
        public <B> Maybe<S, B> thenOptional(LensGetter<A, Optional<B>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<B> childClass = (Class<B>) RecordLensBuilder.optionalElementType(getter);
            String component = RecordLensBuilder.componentName(getter);
            var composed = affine.andThen(RecordLensBuilder.optional(child));
            return writable
                    ? Maybe.writable(lookup, rootClass, childClass, path() + "." + component, composed)
                    : Maybe.readOnly(lookup, rootClass, childClass, path() + "." + component, composed);
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
        public <V, X extends V> Maybe<S, X> thenSubtype(LensGetter<A, V> getter, Class<X> subtypeClass) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(subtypeClass);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            String component = RecordLensBuilder.componentName(getter);
            var composed = affine.andThen(RecordLensBuilder.subtype(child, subtypeClass));
            return writable
                    ? Maybe.writable(lookup, rootClass, subtypeClass, path() + "." + component, composed)
                    : Maybe.readOnly(lookup, rootClass, subtypeClass, path() + "." + component, composed);
        }

        /**
         * Returns a zero-or-more path for all elements of a list-valued child
         * component.
         *
         * @param  getter the list child component accessor
         * @param  <E> the list element type
         * @return a path selecting all child list elements when this path focuses a value
         */
        public <E> Many<S, E> thenEach(LensGetter<A, List<E>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<E> childClass = (Class<E>) RecordLensBuilder.listElementType(getter);
            String component = RecordLensBuilder.componentName(getter);
            var childTraversal = child.andThen(Each.listTraversal());
            return writable
                    ? Many.writable(lookup, rootClass, childClass, path() + "." + component, affine.andThen(childTraversal))
                    : Many.readOnly(lookup, rootClass, childClass, path() + "." + component, affine.asFold().andThen(childTraversal.asFold()));
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
        public <K, V> Many<S, V> thenValues(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            String component = RecordLensBuilder.componentName(getter);
            var childTraversal = child.andThen(Traversal.mapValues());
            return writable
                    ? Many.writable(lookup, rootClass, childClass, path() + "." + component, affine.andThen(childTraversal))
                    : Many.readOnly(lookup, rootClass, childClass, path() + "." + component, affine.asFold().andThen(childTraversal.asFold()));
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
        public <K, V> Many<S, K> thenKeys(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<K> childClass = (Class<K>) RecordLensBuilder.mapKeyType(getter);
            String component = RecordLensBuilder.componentName(getter);
            return Many.readOnly(lookup, rootClass, childClass, path() + "." + component, affine.asFold().andThen(child).andThen(Fold.mapKeys()));
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
        public <K, V> Maybe<S, V> thenValue(LensGetter<A, Map<K, V>> getter, K key) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(key);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            String component = RecordLensBuilder.componentName(getter);
            var composed = affine.andThen(child).andThen(Affine.mapValue(key));
            return writable
                    ? Maybe.writable(lookup, rootClass, childClass, path() + "." + component + "[" + key + "]", composed)
                    : Maybe.readOnly(lookup, rootClass, childClass, path() + "." + component + "[" + key + "]", composed);
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
        private final Fold<S, A> fold;
        private final Traversal<S, A> traversal;

        private Many(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Fold<S, A> fold,
                Traversal<S, A> traversal
        ) {
            super(path);
            this.lookup = Objects.requireNonNull(lookup);
            this.rootClass = Objects.requireNonNull(rootClass);
            this.focusClass = Objects.requireNonNull(focusClass);
            this.fold = Objects.requireNonNull(fold);
            this.traversal = traversal;
        }

        private static <S, A> Many<S, A> writable(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Traversal<S, A> traversal
        ) {
            return new Many<>(lookup, rootClass, focusClass, path, traversal.asFold(), traversal);
        }

        private static <S, A> Many<S, A> readOnly(
                MethodHandles.Lookup lookup,
                Class<S> rootClass,
                Class<A> focusClass,
                String path,
                Fold<S, A> fold
        ) {
            return new Many<>(lookup, rootClass, focusClass, path, fold, null);
        }

        /**
         * Returns all focused values from the given source.
         *
         * @param  source the source value
         * @return an immutable list of focused values
         */
        public List<A> getAll(S source) {
            return List.copyOf(fold.getAll(source));
        }

        /**
         * Returns the number of focused values.
         *
         * @param  source the source value
         * @return the number of focused values
         */
        public long count(S source) {
            return fold.length(source);
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
            return fold.exists(predicate, source);
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
            return fold.all(predicate, source);
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
            return fold.findOptional(predicate, source);
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
            return requireTraversal(traversal, path()).modify(updater, source);
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
            return traversal != null
                    ? Many.writable(lookup, rootClass, focusClass, path(), traversal.filtered(predicate))
                    : Many.readOnly(lookup, rootClass, focusClass, path(), fold.filtered(predicate));
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
            return traversal != null
                    ? Maybe.writable(lookup, rootClass, focusClass, path() + "[" + index + "]", traversal.at(index))
                    : Maybe.readOnly(lookup, rootClass, focusClass, path() + "[" + index + "]", fold.at(index));
        }

        /**
         * Returns a nested path for a child component of each focused value.
         *
         * @param  getter the child component accessor
         * @param  <B> the child component type
         * @return a path selecting each child component
         */
        public <B> Many<S, B> then(LensGetter<A, B> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<B> childClass = (Class<B>) RecordLensBuilder.componentType(focusClass, RecordLensBuilder.componentName(getter));
            return traversal != null
                    ? Many.writable(lookup, rootClass, childClass, path() + "." + RecordLensBuilder.componentName(getter), traversal.andThen(child))
                    : Many.readOnly(lookup, rootClass, childClass, path() + "." + RecordLensBuilder.componentName(getter), fold.andThen(child));
        }

        /**
         * Returns a nested path for present values of an
         * {@link Optional}-typed child component of each focused value.
         *
         * @param  getter the optional child component accessor
         * @param  <B> the optional element type
         * @return a path selecting all present child values
         */
        public <B> Many<S, B> thenOptional(LensGetter<A, Optional<B>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<B> childClass = (Class<B>) RecordLensBuilder.optionalElementType(getter);
            var childAffine = RecordLensBuilder.optional(child);
            return traversal != null
                    ? Many.writable(lookup, rootClass, childClass, path() + "." + RecordLensBuilder.componentName(getter), traversal.andThen(childAffine.asTraversal()))
                    : Many.readOnly(lookup, rootClass, childClass, path() + "." + RecordLensBuilder.componentName(getter), fold.andThen(childAffine.asFold()));
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
        public <V, X extends V> Many<S, X> thenSubtype(LensGetter<A, V> getter, Class<X> subtypeClass) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(subtypeClass);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
            var childAffine = RecordLensBuilder.subtype(child, subtypeClass);
            return traversal != null
                    ? Many.writable(lookup, rootClass, subtypeClass, path() + "." + RecordLensBuilder.componentName(getter), traversal.andThen(childAffine.asTraversal()))
                    : Many.readOnly(lookup, rootClass, subtypeClass, path() + "." + RecordLensBuilder.componentName(getter), fold.andThen(childAffine.asFold()));
        }

        /**
         * Returns a nested path for all elements of a list-valued child
         * component of each focused value.
         *
         * @param  getter the list child component accessor
         * @param  <B> the list element type
         * @return a path selecting all child list elements
         */
        public <B> Many<S, B> thenEach(LensGetter<A, List<B>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<B> childClass = (Class<B>) RecordLensBuilder.listElementType(getter);
            String component = RecordLensBuilder.componentName(getter);
            var childTraversal = child.andThen(Each.listTraversal());
            return traversal != null
                    ? Many.writable(lookup, rootClass, childClass, path() + "." + component, traversal.andThen(childTraversal))
                    : Many.readOnly(lookup, rootClass, childClass, path() + "." + component, fold.andThen(childTraversal.asFold()));
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
        public <K, V> Many<S, V> thenValues(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            String component = RecordLensBuilder.componentName(getter);
            var childTraversal = child.andThen(Traversal.mapValues());
            return traversal != null
                    ? Many.writable(lookup, rootClass, childClass, path() + "." + component, traversal.andThen(childTraversal))
                    : Many.readOnly(lookup, rootClass, childClass, path() + "." + component, fold.andThen(childTraversal.asFold()));
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
        public <K, V> Many<S, K> thenKeys(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<K> childClass = (Class<K>) RecordLensBuilder.mapKeyType(getter);
            String component = RecordLensBuilder.componentName(getter);
            return Many.readOnly(lookup, rootClass, childClass, path() + "." + component, (traversal != null)
                    ? traversal.asFold().andThen(child).andThen(Fold.mapKeys())
                    : fold.andThen(child).andThen(Fold.mapKeys()));
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
        public <K, V> Many<S, V> thenValue(LensGetter<A, Map<K, V>> getter, K key) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(key);
            var child = RecordLensBuilder.lens(lookup, focusClass, getter);
    
            Class<V> childClass = (Class<V>) RecordLensBuilder.mapValueType(getter);
            String component = RecordLensBuilder.componentName(getter);
            var childAffine = child.andThen(Affine.mapValue(key));
            return traversal != null
                    ? Many.writable(lookup, rootClass, childClass, path() + "." + component + "[" + key + "]", traversal.andThen(childAffine.asTraversal()))
                    : Many.readOnly(lookup, rootClass, childClass, path() + "." + component + "[" + key + "]", fold.andThen(childAffine.asFold()));
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
            return traversal != null
                    ? Many.writable(lookup, rootClass, subtypeClass, path(), traversal.andThen(Prisms.instanceOf(subtypeClass)))
                    : Many.readOnly(lookup, rootClass, subtypeClass, path(), fold.andThen(Prisms.instanceOf(subtypeClass)));
        }
    }
}
