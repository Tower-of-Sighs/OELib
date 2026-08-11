package cc.sighs.oelib.config;

import com.flechazo.hkt.business.util.OptionalOps;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.Affine;
import com.flechazo.optics.LensGetter;
import com.flechazo.optics.Prism;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.focus.AffinePath;
import com.flechazo.optics.focus.FocusPath;
import com.flechazo.optics.focus.TraversalPath;
import com.flechazo.optics.util.Traversals;

import java.util.*;
import java.util.function.Predicate;

/**
 * Represents a reusable selection within a configuration value.
 *
 * <p>A focus preserves its selection cardinality as exactly one, zero or one,
 * or zero or more values. A composed focus selects values relative to every
 * value selected by its parent.
 *
 * @param <S> the configuration root type
 * @param <A> the focused value type
 */
public sealed abstract class ConfigFocus<S, A>
        permits ConfigFocus.One, ConfigFocus.Maybe, ConfigFocus.Many {
    private final Class<A> focusClass;

    private ConfigFocus(Class<A> focusClass) {
        this.focusClass = Objects.requireNonNull(focusClass, "focusClass");
    }

    /**
     * Returns the runtime class of the focused value.
     *
     * @return the focused value class
     */
    public final Class<A> focusClass() {
        return focusClass;
    }

    @SuppressWarnings("unchecked")
    private static <A> Class<A> componentClass(Class<?> sourceClass, LensGetter<?, A> getter) {
        String component = RecordLensBuilder.componentName(getter);
        return (Class<A>) RecordLensBuilder.componentType(sourceClass, component);
    }

    @SuppressWarnings("unchecked")
    private static <A> Class<A> optionalClass(LensGetter<?, Optional<A>> getter) {
        return (Class<A>) RecordLensBuilder.optionalElementType(getter);
    }

    @SuppressWarnings("unchecked")
    private static <A> Class<A> listClass(LensGetter<?, List<A>> getter) {
        return (Class<A>) RecordLensBuilder.listElementType(getter);
    }

    @SuppressWarnings("unchecked")
    private static <A> Class<A> setClass(LensGetter<?, Set<A>> getter) {
        return (Class<A>) RecordLensBuilder.setElementType(getter);
    }

    @SuppressWarnings("unchecked")
    private static <A> Class<A> arrayClass(LensGetter<?, A[]> getter) {
        return (Class<A>) RecordLensBuilder.arrayElementType(getter);
    }

    @SuppressWarnings("unchecked")
    private static <K> Class<K> mapKeyClass(LensGetter<?, ? extends Map<K, ?>> getter) {
        return (Class<K>) RecordLensBuilder.mapKeyType(getter);
    }

    @SuppressWarnings("unchecked")
    private static <V> Class<V> mapValueClass(LensGetter<?, ? extends Map<?, V>> getter) {
        return (Class<V>) RecordLensBuilder.mapValueType(getter);
    }

    private static <A> Affine<Optional<A>, A> optionalValue() {
        return Affine.of(
                optional -> OptionalOps.toEither(optional, () -> optional),
                (optional, value) -> Optional.of(value)
        );
    }

    /**
     * Represents a focus that selects exactly one value.
     *
     * @param <S> the configuration root type
     * @param <A> the focused value type
     */
    public static final class One<S, A> extends ConfigFocus<S, A> {
        private final FocusPath<S, A> prototype;

        One(Class<A> focusClass, FocusPath<S, A> prototype) {
            super(focusClass);
            this.prototype = Objects.requireNonNull(prototype, "prototype");
        }

        /**
         * Returns the advanced focus representation for this selection.
         *
         * @return the advanced focus representation
         */
        public FocusPath<S, A> prototype() {
            return prototype;
        }

        /**
         * Returns a focus composed with a record component of the selected value.
         *
         * @param getter the accessor identifying the nested component
         * @param <B> the nested component type
         * @return an exactly-one focus for the nested component
         */
        public <B> One<S, B> then(LensGetter<A, B> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = componentClass(focusClass(), getter);
            return new One<>(nextClass, prototype.via(RecordLensBuilder.lens(focusClass(), getter)));
        }

        /**
         * Returns a focus composed with the present value of an optional nested
         * component.
         *
         * @param getter the accessor identifying the optional nested component
         * @param <B> the optional value type
         * @return a zero-or-one focus for the present value
         */
        public <B> Maybe<S, B> thenOptional(LensGetter<A, Optional<B>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = optionalClass(getter);
            return new Maybe<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter).andThen(optionalValue())));
        }

        /**
         * Returns a focus composed with a nested component matching a subtype.
         *
         * @param getter the accessor identifying the nested component
         * @param subtypeClass the subtype selected by the returned focus
         * @param <V> the nested component base type
         * @param <X> the selected subtype
         * @return a zero-or-one focus for a matching nested value
         */
        public <V, X extends V> Maybe<S, X> thenSubtype(
                LensGetter<A, V> getter, Class<X> subtypeClass) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(subtypeClass, "subtypeClass");
            Class<V> baseClass = componentClass(focusClass(), getter);
            return new Maybe<>(subtypeClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Prism.subtype(baseClass, subtypeClass))));
        }

        /**
         * Returns a focus composed with all elements of a nested list component.
         *
         * @param getter the accessor identifying the nested list component
         * @param <B> the list element type
         * @return a multi-focus for the nested list elements
         */
        public <B> Many<S, B> thenListElements(LensGetter<A, List<B>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = listClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forList())));
        }

        /**
         * Returns a focus composed with all elements of a nested set component.
         *
         * @param getter the accessor identifying the nested set component
         * @param <B> the set element type
         * @return a multi-focus for the nested set elements
         */
        public <B> Many<S, B> thenSetElements(LensGetter<A, Set<B>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = setClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forSet())));
        }

        /**
         * Returns a focus composed with all elements of a nested array component.
         *
         * @param getter the accessor identifying the nested array component
         * @param <B> the array element type
         * @return a multi-focus for the nested array elements
         */
        public <B> Many<S, B> thenArrayElements(LensGetter<A, B[]> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = arrayClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forArray(nextClass))));
        }

        /**
         * Returns a focus composed with all values of a nested map component.
         *
         * @param getter the accessor identifying the nested map component
         * @param <K> the map key type
         * @param <V> the map value type
         * @return a multi-focus for the nested map values
         */
        public <K, V> Many<S, V> thenMapValues(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<V> nextClass = mapValueClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forMapValues())));
        }

        /**
         * Returns a focus composed with all entries of a nested map component.
         *
         * @param getter the accessor identifying the nested map component
         * @param <K> the map key type
         * @param <V> the map value type
         * @return a multi-focus for key-value tuples in entry encounter order
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <K, V> Many<S, Tuple2<K, V>> thenMapEntries(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter, "getter");
            return new Many<>((Class) Tuple2.class, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forMapEntries())));
        }

        /**
         * Returns a focus composed with the value associated with a nested map key.
         *
         * @param getter the accessor identifying the nested map component
         * @param key the key whose associated value is selected
         * @param <K> the map key type
         * @param <V> the map value type
         * @return a zero-or-one focus that is empty when the key is absent
         */
        public <K, V> Maybe<S, V> thenMapValue(LensGetter<A, Map<K, V>> getter, K key) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(key, "key");
            Class<V> nextClass = mapValueClass(getter);
            return new Maybe<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Affine.mapValue(key))));
        }

        /**
         * Returns a focus that selects this value only when it matches a subtype.
         *
         * @param subtypeClass the subtype selected by the returned focus
         * @param <X> the selected subtype
         * @return a zero-or-one focus for a matching value
         */
        public <X extends A> Maybe<S, X> asSubtype(Class<X> subtypeClass) {
            Objects.requireNonNull(subtypeClass, "subtypeClass");
            return new Maybe<>(subtypeClass, prototype.via(
                    Prism.subtype(focusClass(), subtypeClass)));
        }
    }

    /**
     * Represents a focus that selects zero or one value.
     *
     * @param <S> the configuration root type
     * @param <A> the focused value type
     */
    public static final class Maybe<S, A> extends ConfigFocus<S, A> {
        private final AffinePath<S, A> prototype;

        Maybe(Class<A> focusClass, AffinePath<S, A> prototype) {
            super(focusClass);
            this.prototype = Objects.requireNonNull(prototype, "prototype");
        }

        /**
         * Returns the advanced affine representation for this selection.
         *
         * @return the advanced affine representation
         */
        public AffinePath<S, A> prototype() {
            return prototype;
        }

        /**
         * Returns a focus composed with a record component of each present value.
         *
         * @param getter the accessor identifying the nested component
         * @param <B> the nested component type
         * @return a zero-or-one focus for the nested component
         */
        public <B> Maybe<S, B> then(LensGetter<A, B> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = componentClass(focusClass(), getter);
            return new Maybe<>(nextClass, prototype.via(RecordLensBuilder.lens(focusClass(), getter)));
        }

        /**
         * Returns a focus composed with the present value of an optional nested
         * component.
         *
         * @param getter the accessor identifying the optional nested component
         * @param <B> the optional value type
         * @return a zero-or-one focus for the present value
         */
        public <B> Maybe<S, B> thenOptional(LensGetter<A, Optional<B>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = optionalClass(getter);
            return new Maybe<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter).andThen(optionalValue())));
        }

        /**
         * Returns a focus composed with a nested component matching a subtype.
         *
         * @param getter the accessor identifying the nested component
         * @param subtypeClass the subtype selected by the returned focus
         * @param <V> the nested component base type
         * @param <X> the selected subtype
         * @return a zero-or-one focus for a matching nested value
         */
        public <V, X extends V> Maybe<S, X> thenSubtype(
                LensGetter<A, V> getter, Class<X> subtypeClass) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(subtypeClass, "subtypeClass");
            Class<V> baseClass = componentClass(focusClass(), getter);
            return new Maybe<>(subtypeClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Prism.subtype(baseClass, subtypeClass))));
        }

        /**
         * Returns a focus composed with all elements of a nested list component.
         *
         * @param getter the accessor identifying the nested list component
         * @param <B> the list element type
         * @return a multi-focus for the nested list elements of present values
         */
        public <B> Many<S, B> thenListElements(LensGetter<A, List<B>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = listClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forList())));
        }

        /**
         * Returns a focus composed with all elements of a nested set component.
         *
         * @param getter the accessor identifying the nested set component
         * @param <B> the set element type
         * @return a multi-focus for the nested set elements of present values
         */
        public <B> Many<S, B> thenSetElements(LensGetter<A, Set<B>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = setClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forSet())));
        }

        /**
         * Returns a focus composed with all elements of a nested array component.
         *
         * @param getter the accessor identifying the nested array component
         * @param <B> the array element type
         * @return a multi-focus for the nested array elements of present values
         */
        public <B> Many<S, B> thenArrayElements(LensGetter<A, B[]> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = arrayClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forArray(nextClass))));
        }

        /**
         * Returns a focus composed with all values of a nested map component.
         *
         * @param getter the accessor identifying the nested map component
         * @param <K> the map key type
         * @param <V> the map value type
         * @return a multi-focus for the nested map values of present values
         */
        public <K, V> Many<S, V> thenMapValues(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<V> nextClass = mapValueClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forMapValues())));
        }

        /**
         * Returns a focus composed with all entries of a nested map component.
         *
         * @param getter the accessor identifying the nested map component
         * @param <K> the map key type
         * @param <V> the map value type
         * @return a multi-focus for key-value tuples in entry encounter order
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <K, V> Many<S, Tuple2<K, V>> thenMapEntries(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter, "getter");
            return new Many<>((Class) Tuple2.class, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forMapEntries())));
        }

        /**
         * Returns a focus composed with the value associated with a nested map key.
         *
         * @param getter the accessor identifying the nested map component
         * @param key the key whose associated value is selected
         * @param <K> the map key type
         * @param <V> the map value type
         * @return a zero-or-one focus that is empty when either selection is absent
         */
        public <K, V> Maybe<S, V> thenMapValue(LensGetter<A, Map<K, V>> getter, K key) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(key, "key");
            Class<V> nextClass = mapValueClass(getter);
            return new Maybe<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Affine.<K, V>mapValue(key))));
        }

        /**
         * Returns a focus that retains present values matching a subtype.
         *
         * @param subtypeClass the subtype selected by the returned focus
         * @param <X> the selected subtype
         * @return a zero-or-one focus for a matching value
         */
        public <X extends A> Maybe<S, X> asSubtype(Class<X> subtypeClass) {
            Objects.requireNonNull(subtypeClass, "subtypeClass");
            return new Maybe<>(subtypeClass, prototype.via(
                    Prism.subtype(focusClass(), subtypeClass)));
        }
    }

    /**
     * Represents a focus that selects zero or more values.
     *
     * @param <S> the configuration root type
     * @param <A> the focused value type
     */
    public static final class Many<S, A> extends ConfigFocus<S, A> {
        private final TraversalPath<S, A> prototype;

        Many(Class<A> focusClass, TraversalPath<S, A> prototype) {
            super(focusClass);
            this.prototype = Objects.requireNonNull(prototype, "prototype");
        }

        /**
         * Returns the advanced traversal representation for this selection.
         *
         * @return the advanced traversal representation
         */
        public TraversalPath<S, A> prototype() {
            return prototype;
        }

        /**
         * Returns a multi-focus retaining selected values that satisfy a predicate.
         *
         * @param predicate the condition used to retain selected values
         * @return a multi-focus containing only matching values
         */
        public Many<S, A> filter(Predicate<? super A> predicate) {
            Objects.requireNonNull(predicate, "predicate");
            return new Many<>(focusClass(), TraversalPath.of(
                    Traversal.from(prototype.toTraversal()).filtered(predicate)));
        }

        /**
         * Returns a zero-or-one focus for a selected value at an encounter index.
         *
         * @param encounterIndex the zero-based index in selection encounter order
         * @return a focus that is empty for a negative or out-of-range index
         */
        public Maybe<S, A> at(int encounterIndex) {
            return new Maybe<>(focusClass(), AffinePath.of(
                    Traversal.from(prototype.toTraversal()).at(encounterIndex)));
        }

        /**
         * Returns a multi-focus composed with a record component of each
         * selected value.
         *
         * @param getter the accessor identifying the nested component
         * @param <B> the nested component type
         * @return a multi-focus for the nested components
         */
        public <B> Many<S, B> then(LensGetter<A, B> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = componentClass(focusClass(), getter);
            return new Many<>(nextClass, prototype.via(RecordLensBuilder.lens(focusClass(), getter)));
        }

        /**
         * Returns a multi-focus composed with present values of optional nested
         * components.
         *
         * @param getter the accessor identifying the optional nested component
         * @param <B> the optional value type
         * @return a multi-focus for all present nested values
         */
        public <B> Many<S, B> thenOptional(LensGetter<A, Optional<B>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = optionalClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(optionalValue()).asTraversal()));
        }

        /**
         * Returns a multi-focus composed with nested components matching a subtype.
         *
         * @param getter the accessor identifying the nested component
         * @param subtypeClass the subtype retained by the returned focus
         * @param <V> the nested component base type
         * @param <X> the selected subtype
         * @return a multi-focus for matching nested values
         */
        public <V, X extends V> Many<S, X> thenSubtype(
                LensGetter<A, V> getter, Class<X> subtypeClass) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(subtypeClass, "subtypeClass");
            Class<V> baseClass = componentClass(focusClass(), getter);
            return new Many<>(subtypeClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Prism.subtype(baseClass, subtypeClass))
                            .asTraversal()));
        }

        /**
         * Returns a multi-focus composed with all elements of nested list components.
         *
         * @param getter the accessor identifying the nested list component
         * @param <B> the list element type
         * @return a multi-focus for all nested list elements
         */
        public <B> Many<S, B> thenListElements(LensGetter<A, List<B>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = listClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forList())));
        }

        /**
         * Returns a multi-focus composed with all elements of nested set components.
         *
         * @param getter the accessor identifying the nested set component
         * @param <B> the set element type
         * @return a multi-focus for all nested set elements
         */
        public <B> Many<S, B> thenSetElements(LensGetter<A, Set<B>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = setClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forSet())));
        }

        /**
         * Returns a multi-focus composed with all elements of nested array components.
         *
         * @param getter the accessor identifying the nested array component
         * @param <B> the array element type
         * @return a multi-focus for all nested array elements
         */
        public <B> Many<S, B> thenArrayElements(LensGetter<A, B[]> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<B> nextClass = arrayClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forArray(nextClass))));
        }

        /**
         * Returns a multi-focus composed with all values of nested map components.
         *
         * @param getter the accessor identifying the nested map component
         * @param <K> the map key type
         * @param <V> the map value type
         * @return a multi-focus for all nested map values
         */
        public <K, V> Many<S, V> thenMapValues(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter, "getter");
            Class<V> nextClass = mapValueClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forMapValues())));
        }

        /**
         * Returns a multi-focus composed with all entries of nested map components.
         *
         * @param getter the accessor identifying the nested map component
         * @param <K> the map key type
         * @param <V> the map value type
         * @return a multi-focus for key-value tuples in encounter order
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <K, V> Many<S, Tuple2<K, V>> thenMapEntries(LensGetter<A, Map<K, V>> getter) {
            Objects.requireNonNull(getter, "getter");
            return new Many<>((Class) Tuple2.class, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Traversals.forMapEntries())));
        }

        /**
         * Returns a multi-focus composed with values associated with a nested map key.
         *
         * @param getter the accessor identifying the nested map component
         * @param key the key whose associated values are selected
         * @param <K> the map key type
         * @param <V> the map value type
         * @return a multi-focus containing one value for each selected map that
         *         contains the key
         */
        public <K, V> Many<S, V> thenMapValue(LensGetter<A, Map<K, V>> getter, K key) {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(key, "key");
            Class<V> nextClass = mapValueClass(getter);
            return new Many<>(nextClass, prototype.via(
                    RecordLensBuilder.lens(focusClass(), getter)
                            .andThen(Affine.<K, V>mapValue(key).asTraversal())));
        }

        /**
         * Returns a multi-focus retaining selected values matching a subtype.
         *
         * @param subtypeClass the subtype retained by the returned focus
         * @param <X> the selected subtype
         * @return a multi-focus for matching values
         */
        public <X extends A> Many<S, X> asSubtype(Class<X> subtypeClass) {
            Objects.requireNonNull(subtypeClass, "subtypeClass");
            return new Many<>(subtypeClass, prototype.via(
                    Prism.subtype(focusClass(), subtypeClass)));
        }
    }
}
