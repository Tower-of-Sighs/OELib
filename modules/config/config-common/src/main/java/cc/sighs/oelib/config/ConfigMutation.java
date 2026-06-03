package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.internal.Traversals;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A pure function that transforms a configuration value of type {@code S}
 * into a new value of the same type.
 *
 * <p>Mutations are composed and applied in batch via
 * {@link ConfigUnit#updateAll(ConfigMutation[])} or
 * {@link ConfigUnitOps#updateAllNoSave(ConfigUnit, ConfigMutation[])}.
 * Use the factory methods on this class to create mutations from record
 * component getters.
 *
 * @param <S> the type of the configuration value
 */
@FunctionalInterface
public interface ConfigMutation<S> {
    /**
     * Applies this mutation to the given source value.
     *
     * @param source the current configuration value
     * @return the transformed configuration value
     */
    S apply(S source);

    // -- Lens factories --

    /**
     * Creates a mutation that sets a record component to a fixed value.
     *
     * @param getter a serializable method reference to a record component
     * @param value  the new value
     * @param <S>    the configuration type
     * @param <V>    the field value type
     * @return a mutation that sets the field
     */
    static <S, V> ConfigMutation<S> set(RecordLensBuilder.LensGetter<S, V> getter, V value) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(value);
        return source -> {
            @SuppressWarnings("unchecked")
            Class<S> rc = (Class<S>) source.getClass();
            var lens = RecordLensBuilder.lens(rc, getter);
            return lens.set(source, value);
        };
    }

    /**
     * Creates a mutation that transforms a record component.
     *
     * @param getter   a serializable method reference to a record component
     * @param modifier a function transforming the current field value
     * @param <S>      the configuration type
     * @param <V>      the field value type
     * @return a mutation that transforms the field
     */
    static <S, V> ConfigMutation<S> map(RecordLensBuilder.LensGetter<S, V> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        return source -> {
            @SuppressWarnings("unchecked")
            Class<S> rc = (Class<S>) source.getClass();
            var lens = RecordLensBuilder.lens(rc, getter);
            return lens.update(source, modifier);
        };
    }

    // -- Prism factories --

    /**
     * Creates a mutation that transforms an {@code Optional} field only if
     * a value is present.
     *
     * @param getter   a serializable method reference to an {@code Optional} record component
     * @param modifier a function transforming the present value
     * @param <S>      the configuration type
     * @param <V>      the optional value type
     * @return a mutation that conditionally transforms the field
     */
    static <S, V> ConfigMutation<S> ifPresent(RecordLensBuilder.LensGetter<S, Optional<V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        return source -> {
            @SuppressWarnings("unchecked")
            Class<S> rc = (Class<S>) source.getClass();
            var lens = RecordLensBuilder.lens(rc, getter);
            var prism = RecordLensBuilder.optional(lens);
            return prism.updateIfPresent(source, modifier);
        };
    }

    /**
     * Creates a mutation that transforms a field only when its runtime value
     * is an instance of the given subtype.
     *
     * @param getter   a serializable method reference to a record component
     * @param subtype  the expected subtype class
     * @param modifier a function transforming the matched value
     * @param <S>      the configuration type
     * @param <V>      the base field type
     * @param <X>      the subtype to match
     * @return a mutation that conditionally transforms the field
     */
    static <S, V, X extends V> ConfigMutation<S> ifPresent(RecordLensBuilder.LensGetter<S, V> getter, Class<X> subtype, UnaryOperator<X> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(subtype);
        Objects.requireNonNull(modifier);
        return source -> {
            @SuppressWarnings("unchecked")
            Class<S> rc = (Class<S>) source.getClass();
            var lens = RecordLensBuilder.lens(rc, getter);
            var prism = RecordLensBuilder.subtype(lens, subtype);
            return prism.updateIfPresent(source, modifier);
        };
    }

    // -- Traversal factories --

    /**
     * Creates a mutation that transforms all elements of a {@code List} field.
     *
     * @param getter   a serializable method reference to a list field accessor
     * @param modifier a function transforming each list element
     * @param <S>      the configuration type
     * @param <T>      the list element type
     * @return a mutation that transforms all list elements
     */
    static <S, T> ConfigMutation<S> updateElements(RecordLensBuilder.LensGetter<S, List<T>> getter, UnaryOperator<T> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        return source -> {
            @SuppressWarnings("unchecked")
            Class<S> rc = (Class<S>) source.getClass();
            var lens = RecordLensBuilder.lens(rc, getter);
            var traversal = Traversals.onList(lens);
            return traversal.update(source, modifier);
        };
    }

    /**
     * Creates a mutation that transforms only the elements of a {@code List}
     * field that satisfy the given predicate.
     *
     * @param getter    a serializable method reference to a list field accessor
     * @param predicate a predicate selecting which elements to transform
     * @param modifier  a function transforming each selected element
     * @param <S>       the configuration type
     * @param <T>       the list element type
     * @return a mutation that conditionally transforms list elements
     */
    static <S, T> ConfigMutation<S> updateWhere(RecordLensBuilder.LensGetter<S, List<T>> getter, Predicate<T> predicate, UnaryOperator<T> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(modifier);
        return source -> {
            @SuppressWarnings("unchecked")
            Class<S> rc = (Class<S>) source.getClass();
            var lens = RecordLensBuilder.lens(rc, getter);
            var traversal = Traversals.onList(lens).filter(predicate);
            return traversal.update(source, modifier);
        };
    }

    /**
     * Creates a mutation that transforms all values of a {@code Map} field.
     *
     * @param getter   a serializable method reference to a map field accessor
     * @param modifier a function transforming each map value
     * @param <S>      the configuration type
     * @param <K>      the map key type
     * @param <V>      the map value type
     * @return a mutation that transforms all map values
     */
    static <S, K, V> ConfigMutation<S> updateValues(RecordLensBuilder.LensGetter<S, Map<K, V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        return source -> {
            @SuppressWarnings("unchecked")
            Class<S> rc = (Class<S>) source.getClass();
            var lens = RecordLensBuilder.lens(rc, getter);
            var traversal = Traversals.onMapValues(lens);
            return traversal.update(source, modifier);
        };
    }
}
