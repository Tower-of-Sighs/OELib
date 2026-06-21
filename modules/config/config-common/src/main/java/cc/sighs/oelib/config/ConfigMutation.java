package cc.sighs.oelib.config;

import com.flechazo.optics.generated.LensGetter;

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

    /**
     * Returns factories that create path-based mutations.
     *
     * @return path-based mutation factories
     */
    static Paths paths() {
        return Paths.INSTANCE;
    }

    // -- Getter factories --

    /**
     * Creates a mutation that sets a record component to a fixed value.
     *
     * @param getter a serializable method reference to a record component
     * @param value  the new value
     * @param <S>    the configuration type
     * @param <V>    the field value type
     * @return a mutation that sets the field
     */
    static <S, V> ConfigMutation<S> set(LensGetter<S, V> getter, V value) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(value);
        return (ContextualMutation<S>) (source, resolver) -> {
            var lens = resolver.lens(getter);
            return lens.set(value, source);
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
    static <S, V> ConfigMutation<S> map(LensGetter<S, V> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        return (ContextualMutation<S>) (source, resolver) -> {
            var lens = resolver.lens(getter);
            return lens.modify(modifier, source);
        };
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
    static <S, V> ConfigMutation<S> ifPresent(LensGetter<S, Optional<V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        return (ContextualMutation<S>) (source, resolver) -> {
            var affine = resolver.optional(getter);
            return affine.modify(modifier, source);
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
    static <S, V, X extends V> ConfigMutation<S> whenSubtype(LensGetter<S, V> getter, Class<X> subtype, UnaryOperator<X> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(subtype);
        Objects.requireNonNull(modifier);
        return (ContextualMutation<S>) (source, resolver) -> {
            var affine = resolver.subtype(getter, subtype);
            return affine.modify(modifier, source);
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
    static <S, T> ConfigMutation<S> updateElements(LensGetter<S, List<T>> getter, UnaryOperator<T> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        return (ContextualMutation<S>) (source, resolver) -> {
            var traversal = resolver.listTraversal(getter);
            return traversal.modify(modifier, source);
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
    static <S, T> ConfigMutation<S> updateWhere(LensGetter<S, List<T>> getter, Predicate<T> predicate, UnaryOperator<T> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(modifier);
        return (ContextualMutation<S>) (source, resolver) -> {
            var traversal = resolver.listTraversal(getter).filtered(predicate);
            return traversal.modify(modifier, source);
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
    static <S, K, V> ConfigMutation<S> updateValues(LensGetter<S, Map<K, V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        return (ContextualMutation<S>) (source, resolver) -> {
            var traversal = resolver.mapValuesTraversal(getter);
            return traversal.modify(modifier, source);
        };
    }

    /**
     * Creates a mutation that transforms only the values of a {@code Map}
     * field that satisfy the given predicate.
     *
     * @param getter    a serializable method reference to a map field accessor
     * @param predicate a predicate selecting which values to transform
     * @param modifier  a function transforming each selected value
     * @param <S>       the configuration type
     * @param <K>       the map key type
     * @param <V>       the map value type
     * @return a mutation that conditionally transforms map values
     */
    static <S, K, V> ConfigMutation<S> updateValuesWhere(LensGetter<S, Map<K, V>> getter, Predicate<V> predicate, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(modifier);
        return (ContextualMutation<S>) (source, resolver) -> {
            var traversal = resolver.mapValuesTraversal(getter).filtered(predicate);
            return traversal.modify(modifier, source);
        };
    }

    /**
     * Creates mutations from preconstructed paths.
     */
    final class Paths {
        private static final Paths INSTANCE = new Paths();

        private Paths() {
        }

        /**
         * Creates a mutation that sets the value selected by an exactly-one
         * path.
         *
         * @param  path the path selecting exactly one value
         * @param  value the replacement value
         * @param  <S> the configuration type
         * @param  <V> the focused value type
         * @return a mutation that sets the focused value
         */
        public <S, V> ConfigMutation<S> set(ConfigPath.One<S, V> path, V value) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(value);
            return source -> path.set(source, value);
        }

        /**
         * Creates a mutation that transforms the value selected by an
         * exactly-one path.
         *
         * @param  path the path selecting exactly one value
         * @param  modifier the function that transforms the focused value
         * @param  <S> the configuration type
         * @param  <V> the focused value type
         * @return a mutation that transforms the focused value
         */
        public <S, V> ConfigMutation<S> map(ConfigPath.One<S, V> path, UnaryOperator<V> modifier) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(modifier);
            return source -> path.update(source, modifier);
        }

        /**
         * Creates a mutation that transforms the value selected by a
         * zero-or-one path when it is present.
         *
         * @param  path the path selecting zero or one value
         * @param  modifier the function that transforms the focused value
         * @param  <S> the configuration type
         * @param  <V> the focused value type
         * @return a mutation that conditionally transforms the focused value
         */
        public <S, V> ConfigMutation<S> ifPresent(ConfigPath.Maybe<S, V> path, UnaryOperator<V> modifier) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(modifier);
            return source -> path.updateIfPresent(source, modifier);
        }

        /**
         * Creates a mutation that transforms the value selected by an
         * exactly-one path only when it has the specified runtime type.
         *
         * @param  path the path selecting exactly one value
         * @param  subtype the expected subtype class
         * @param  modifier the function that transforms the matched value
         * @param  <S> the configuration type
         * @param  <V> the base focused value type
         * @param  <X> the subtype to match
         * @return a mutation that conditionally transforms the focused value
         */
        public <S, V, X extends V> ConfigMutation<S> whenSubtype(ConfigPath.One<S, V> path, Class<X> subtype, UnaryOperator<X> modifier) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(subtype);
            Objects.requireNonNull(modifier);
            return source -> {
                V focused = path.view(source);
                return subtype.isInstance(focused)
                        ? path.set(source, modifier.apply(subtype.cast(focused)))
                        : source;
            };
        }

        /**
         * Creates a mutation that transforms all values selected by a
         * zero-or-more path.
         *
         * @param  path the path selecting zero or more values
         * @param  modifier the function that transforms each focused value
         * @param  <S> the configuration type
         * @param  <V> the focused value type
         * @return a mutation that transforms all focused values
         */
        public <S, V> ConfigMutation<S> updateEach(ConfigPath.Many<S, V> path, UnaryOperator<V> modifier) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(modifier);
            return source -> path.updateEach(source, modifier);
        }

        /**
         * Creates a mutation that transforms selected values that satisfy the
         * given predicate.
         *
         * @param  path the path selecting zero or more values
         * @param  predicate the predicate that selects values to transform
         * @param  modifier the function that transforms each selected value
         * @param  <S> the configuration type
         * @param  <V> the focused value type
         * @return a mutation that conditionally transforms focused values
         */
        public <S, V> ConfigMutation<S> updateWhere(ConfigPath.Many<S, V> path, Predicate<? super V> predicate, UnaryOperator<V> modifier) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(predicate);
            Objects.requireNonNull(modifier);
            return source -> path.where(predicate).updateEach(source, modifier);
        }
    }
}
