package cc.sighs.oelib.config;

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
        return (ContextualMutation<S>) (source, resolver) -> {
            var lens = resolver.lens(getter);
            return lens.set(source, value);
        };
    }

    /**
     * Creates a mutation that sets the value selected by the given path.
     *
     * @param  path the path selecting exactly one value
     * @param  value the replacement value
     * @param  <S> the configuration type
     * @param  <V> the focused value type
     * @return a mutation that sets the focused value
     */
    static <S, V> ConfigMutation<S> set(ConfigPath.One<S, V> path, V value) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(value);
        return source -> path.set(source, value);
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
        return (ContextualMutation<S>) (source, resolver) -> {
            var lens = resolver.lens(getter);
            return lens.update(source, modifier);
        };
    }

    /**
     * Creates a mutation that transforms the value selected by the given path.
     *
     * @param  path the path selecting exactly one value
     * @param  modifier the function that transforms the focused value
     * @param  <S> the configuration type
     * @param  <V> the focused value type
     * @return a mutation that transforms the focused value
     */
    static <S, V> ConfigMutation<S> map(ConfigPath.One<S, V> path, UnaryOperator<V> modifier) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(modifier);
        return source -> path.update(source, modifier);
    }

    // -- Affine factories --

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
        return (ContextualMutation<S>) (source, resolver) -> {
            var affine = resolver.optional(getter);
            return affine.updateIfPresent(source, modifier);
        };
    }

    /**
     * Creates a mutation that transforms the value selected by the given
     * zero-or-one path when it is present.
     *
     * @param  path the path selecting zero or one value
     * @param  modifier the function that transforms the focused value
     * @param  <S> the configuration type
     * @param  <V> the focused value type
     * @return a mutation that conditionally transforms the focused value
     */
    static <S, V> ConfigMutation<S> ifPresent(ConfigPath.Maybe<S, V> path, UnaryOperator<V> modifier) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(modifier);
        return source -> path.updateIfPresent(source, modifier);
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
        return (ContextualMutation<S>) (source, resolver) -> {
            var affine = resolver.subtype(getter, subtype);
            return affine.updateIfPresent(source, modifier);
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
        return (ContextualMutation<S>) (source, resolver) -> {
            var traversal = resolver.listTraversal(getter);
            return traversal.update(source, modifier);
        };
    }

    /**
     * Creates a mutation that transforms all values selected by the given path.
     *
     * @param  path the path selecting zero or more values
     * @param  modifier the function that transforms each focused value
     * @param  <S> the configuration type
     * @param  <T> the focused value type
     * @return a mutation that transforms all focused values
     */
    static <S, T> ConfigMutation<S> updateEach(ConfigPath.Many<S, T> path, UnaryOperator<T> modifier) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(modifier);
        return source -> path.updateEach(source, modifier);
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
        return (ContextualMutation<S>) (source, resolver) -> {
            var traversal = resolver.listTraversal(getter).filter(predicate);
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
        return (ContextualMutation<S>) (source, resolver) -> {
            var traversal = resolver.mapValuesTraversal(getter);
            return traversal.update(source, modifier);
        };
    }
}
