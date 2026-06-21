package cc.sighs.oelib.config;

import com.flechazo.optics.generated.LensGetter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;

/**
 * Utility operations on {@link ConfigUnit} instances that offer finer
 * control over persistence and batching.
 *
 * <p>The "NoSave" variants (for example {@link #updateNoSave(ConfigUnit, LensGetter, UnaryOperator)})
 * modify the in-memory value and fire change events but do not persist to
 * disk, allowing callers to defer persistence until a batch is complete.
 *
 * <p>The {@link #withBatch(ConfigUnit, Consumer)} and
 * {@link #withBatchNoSave(ConfigUnit, Consumer)} methods provide a
 * {@link BatchMutator} that accumulates multiple mutations before applying
 * them as a single change.
 */
public final class ConfigUnitOps {
    private ConfigUnitOps() {
    }

    /**
     * Represents an operation on a boolean value that produces a boolean
     * result.
     */
    @FunctionalInterface
    public interface BooleanUnaryOperator {
        /**
         * Applies this operation to the given value.
         *
         * @param  value the input value
         * @return the operation result
         */
        boolean applyAsBoolean(boolean value);
    }

    /**
     * Returns no-save operations that apply preconstructed paths to the given
     * unit.
     *
     * @param unit the configuration unit
     * @param <T>  the configuration type
     * @return path-based no-save operations for {@code unit}
     */
    public static <T> PathOps<T> paths(ConfigUnit<T> unit) {
        return new PathOps<>(unit);
    }

    /**
     * Updates a field through a getter without persisting to disk.
     *
     * @param unit    the configuration unit
     * @param getter  a serializable method reference to a record component
     * @param updater a function transforming the current field value
     * @param <T>     the configuration type
     * @param <V>     the field value type
     * @return the committed configuration value
     */
    public static <T, V> T updateNoSave(ConfigUnit<T> unit, LensGetter<T, V> getter, UnaryOperator<V> updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        var lens = unit.resolver().lens(getter);
        T current = unit.get();
        T updated = lens.modify(updater, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Sets a field through a getter, persists, and returns the committed field value.
     *
     * @param unit   the configuration unit
     * @param getter a serializable method reference to a record component
     * @param value  the new field value
     * @param <T>    the configuration type
     * @param <V>    the field value type
     * @return the committed field value
     */
    public static <T, V> V setAndGet(ConfigUnit<T> unit, LensGetter<T, V> getter, V value) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        var lens = unit.resolver().lens(getter);
        T current = unit.get();
        T updated = lens.set(value, current);
        T committed = unit.commitCandidate(current, updated, true);
        return lens.get(committed);
    }

    /**
     * Sets a field through a getter without persisting and returns the
     * committed field value.
     *
     * @param unit   the configuration unit
     * @param getter a serializable method reference to a record component
     * @param value  the new field value
     * @param <T>    the configuration type
     * @param <V>    the field value type
     * @return the committed field value
     */
    public static <T, V> V setAndGetNoSave(ConfigUnit<T> unit, LensGetter<T, V> getter, V value) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        var lens = unit.resolver().lens(getter);
        T current = unit.get();
        T updated = lens.set(value, current);
        T committed = unit.commitCandidate(current, updated, false);
        return lens.get(committed);
    }

    /**
     * Updates an {@code int} field through a getter without persisting.
     *
     * @param unit    the configuration unit
     * @param getter  a serializable method reference to an {@code int} record component
     * @param updater a function transforming the current value
     * @param <T>     the configuration type
     * @return the committed configuration value
     */
    public static <T> T updateIntNoSave(ConfigUnit<T> unit, LensGetter<T, Integer> getter, IntUnaryOperator updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        var lens = unit.resolver().lens(getter);
        T current = unit.get();
        T updated = lens.modify(updater::applyAsInt, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Updates a {@code long} field through a getter without persisting.
     *
     * @param unit    the configuration unit
     * @param getter  a serializable method reference to a {@code long} record component
     * @param updater a function transforming the current value
     * @param <T>     the configuration type
     * @return the committed configuration value
     */
    public static <T> T updateLongNoSave(ConfigUnit<T> unit, LensGetter<T, Long> getter, LongUnaryOperator updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        var lens = unit.resolver().lens(getter);
        T current = unit.get();
        T updated = lens.modify(updater::applyAsLong, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Updates a {@code double} field through a getter without persisting.
     *
     * @param unit    the configuration unit
     * @param getter  a serializable method reference to a {@code double} record component
     * @param updater a function transforming the current value
     * @param <T>     the configuration type
     * @return the committed configuration value
     */
    public static <T> T updateDoubleNoSave(ConfigUnit<T> unit, LensGetter<T, Double> getter, DoubleUnaryOperator updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        var lens = unit.resolver().lens(getter);
        T current = unit.get();
        T updated = lens.modify(updater::applyAsDouble, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Updates a {@code boolean} field through a getter without persisting.
     *
     * @param unit    the configuration unit
     * @param getter  a serializable method reference to a {@code boolean} record component
     * @param updater a function transforming the current value
     * @param <T>     the configuration type
     * @return the committed configuration value
     */
    public static <T> T updateBooleanNoSave(ConfigUnit<T> unit, LensGetter<T, Boolean> getter, BooleanUnaryOperator updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        var lens = unit.resolver().lens(getter);
        T current = unit.get();
        T updated = lens.modify(updater::applyAsBoolean, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Applies multiple mutations without persisting to disk.
     *
     * <p>Null mutations in the array are silently skipped.
     *
     * @param unit      the configuration unit
     * @param mutations the mutations to apply
     * @param <T>       the configuration type
     * @return the committed configuration value
     */
    @SafeVarargs
    public static <T> T updateAllNoSave(ConfigUnit<T> unit, ConfigMutation<T>... mutations) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(mutations);
        T current = unit.get();
        T updated = current;
        for (ConfigMutation<T> mutation : mutations) {
            if (mutation == null) {
                continue;
            }
            if (mutation instanceof ContextualMutation<T> contextual) {
                updated = contextual.apply(updated, unit.resolver());
            } else {
                updated = mutation.apply(updated);
            }
        }
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Conditionally updates an {@link Optional} field without persisting.
     *
     * @param unit the configuration unit
     * @param getter a serializable method reference to an {@code Optional} record component
     * @param updater a function transforming the present value
     * @param <T> the configuration type
     * @param <V> the optional value type
     * @return the committed (or unchanged) configuration value
     */
    public static <T, V> T ifPresentNoSave(ConfigUnit<T> unit, LensGetter<T, Optional<V>> getter, UnaryOperator<V> updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        var selector = unit.resolver().optional(getter);
        T current = unit.get();
        T updated = selector.modify(updater, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Conditionally updates a field when its runtime value is an instance of
     * the specified subtype, without persisting.
     *
     * @param unit the configuration unit
     * @param getter a serializable method reference to a record component
     * @param subtype the expected subtype class
     * @param updater a function transforming the matched value
     * @param <T> the configuration type
     * @param <V> the base field type
     * @param <X> the subtype to match
     * @return the committed (or unchanged) configuration value
     */
    public static <T, V, X extends V> T whenSubtypeNoSave(
            ConfigUnit<T> unit,
            LensGetter<T, V> getter,
            Class<X> subtype,
            UnaryOperator<X> updater
    ) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(subtype);
        Objects.requireNonNull(updater);
        var selector = unit.resolver().subtype(getter, subtype);
        T current = unit.get();
        T updated = selector.modify(updater, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Transforms every element of the {@code List} field identified by the
     * given getter without persisting.
     *
     * @param unit the configuration unit
     * @param getter a serializable method reference to the list field accessor
     * @param modifier a function that transforms each list element
     * @param <T> the configuration type
     * @param <V> the list element type
     * @return the committed configuration value
     */
    public static <T, V> T updateElementsNoSave(ConfigUnit<T> unit, LensGetter<T, List<V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        var traversal = unit.resolver().listTraversal(getter);
        T current = unit.get();
        T updated = traversal.modify(modifier, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Transforms the elements of the {@code List} field that satisfy the given
     * predicate without persisting.
     *
     * @param unit the configuration unit
     * @param getter a serializable method reference to the list field accessor
     * @param predicate a predicate that selects which elements to transform
     * @param modifier a function that transforms each selected element
     * @param <T> the configuration type
     * @param <V> the list element type
     * @return the committed configuration value
     */
    public static <T, V> T updateWhereNoSave(
            ConfigUnit<T> unit,
            LensGetter<T, List<V>> getter,
            Predicate<V> predicate,
            UnaryOperator<V> modifier
    ) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(modifier);
        var traversal = unit.resolver().listTraversal(getter).filtered(predicate);
        T current = unit.get();
        T updated = traversal.modify(modifier, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Transforms every value of the {@code Map} field identified by the given
     * getter without persisting.
     *
     * @param unit the configuration unit
     * @param getter a serializable method reference to the map field accessor
     * @param modifier a function that transforms each map value
     * @param <T> the configuration type
     * @param <K> the map key type
     * @param <V> the map value type
     * @return the committed configuration value
     */
    public static <T, K, V> T updateValuesNoSave(ConfigUnit<T> unit, LensGetter<T, Map<K, V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        var traversal = unit.resolver().mapValuesTraversal(getter);
        T current = unit.get();
        T updated = traversal.modify(modifier, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Transforms the values of the {@code Map} field that satisfy the given
     * predicate without persisting.
     *
     * @param unit the configuration unit
     * @param getter a serializable method reference to the map field accessor
     * @param predicate a predicate that selects which values to transform
     * @param modifier a function that transforms each selected value
     * @param <T> the configuration type
     * @param <K> the map key type
     * @param <V> the map value type
     * @return the committed configuration value
     */
    public static <T, K, V> T updateValuesWhereNoSave(
            ConfigUnit<T> unit,
            LensGetter<T, Map<K, V>> getter,
            Predicate<V> predicate,
            UnaryOperator<V> modifier
    ) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(modifier);
        var traversal = unit.resolver().mapValuesTraversal(getter).filtered(predicate);
        T current = unit.get();
        T updated = traversal.modify(modifier, current);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Executes a batch of mutations through a {@link BatchMutator}, persists
     * if any change was made, and returns the final value.
     *
     * @param unit        the configuration unit
     * @param batchAction a consumer that populates the batch mutator
     * @param <T>         the configuration type
     * @return the final configuration value after the batch
     */
    public static <T> T withBatch(ConfigUnit<T> unit, Consumer<BatchMutator<T>> batchAction) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(batchAction);
        BatchMutator<T> batch = withBatchNoSave(unit, batchAction);
        if (batch.changed()) {
            unit.save();
        }
        return batch.value();
    }

    /**
     * Executes a batch of mutations through a {@link BatchMutator} without
     * persisting to disk.
     *
     * @param unit        the configuration unit
     * @param batchAction a consumer that populates the batch mutator
     * @param <T>         the configuration type
     * @return the batch mutator after all mutations have been applied
     */
    public static <T> BatchMutator<T> withBatchNoSave(ConfigUnit<T> unit, Consumer<BatchMutator<T>> batchAction) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(batchAction);
        T current = unit.get();
        BatchMutator<T> batch = new BatchMutator<>(current, unit.resolver());
        batchAction.accept(batch);
        if (batch.changed()) {
            unit.commitCandidate(current, batch.value(), false);
        }
        return batch;
    }

    /**
     * Provides no-save operations for preconstructed paths.
     *
     * @param <T> the configuration type
     */
    public static final class PathOps<T> {
        private final ConfigUnit<T> unit;

        private PathOps(ConfigUnit<T> unit) {
            this.unit = Objects.requireNonNull(unit);
        }

        /**
         * Updates the value selected by an exactly-one path without persisting.
         *
         * @param  path the path selecting exactly one value
         * @param  updater the function that transforms the focused value
         * @param  <V> the focused value type
         * @return the committed configuration value
         */
        public <V> T updateNoSave(ConfigPath.One<T, V> path, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(updater);
            T current = unit.get();
            T updated = path.update(current, updater);
            return unit.commitCandidate(current, updated, false);
        }

        /**
         * Sets the value selected by an exactly-one path without persisting and
         * returns the committed focused value.
         *
         * @param  path the path selecting exactly one value
         * @param  value the replacement value
         * @param  <V> the focused value type
         * @return the committed focused value
         */
        public <V> V setAndGetNoSave(ConfigPath.One<T, V> path, V value) {
            Objects.requireNonNull(path);
            T current = unit.get();
            T updated = path.set(current, value);
            T committed = unit.commitCandidate(current, updated, false);
            return path.view(committed);
        }

        /**
         * Updates the value selected by a zero-or-one path when it is present,
         * without persisting.
         *
         * @param  path the path selecting zero or one value
         * @param  updater the function that transforms the focused value
         * @param  <V> the focused value type
         * @return the committed configuration value
         */
        public <V> T ifPresentNoSave(ConfigPath.Maybe<T, V> path, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(updater);
            T current = unit.get();
            T updated = path.updateIfPresent(current, updater);
            return unit.commitCandidate(current, updated, false);
        }

        /**
         * Updates the value selected by an exactly-one path when it has the
         * specified runtime type, without persisting.
         *
         * @param  path the path selecting exactly one value
         * @param  subtype the expected subtype class
         * @param  updater the function that transforms the matched value
         * @param  <V> the base focused value type
         * @param  <X> the subtype to match
         * @return the committed configuration value
         */
        public <V, X extends V> T whenSubtypeNoSave(ConfigPath.One<T, V> path, Class<X> subtype, UnaryOperator<X> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(subtype);
            Objects.requireNonNull(updater);
            T current = unit.get();
            V focused = path.view(current);
            T updated = subtype.isInstance(focused)
                    ? path.set(current, updater.apply(subtype.cast(focused)))
                    : current;
            return unit.commitCandidate(current, updated, false);
        }

        /**
         * Updates all values selected by a zero-or-more path without
         * persisting.
         *
         * @param  path the path selecting zero or more values
         * @param  updater the function that transforms each focused value
         * @param  <V> the focused value type
         * @return the committed configuration value
         */
        public <V> T updateEachNoSave(ConfigPath.Many<T, V> path, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(updater);
            T current = unit.get();
            T updated = path.updateEach(current, updater);
            return unit.commitCandidate(current, updated, false);
        }

        /**
         * Updates selected values that satisfy the given predicate without
         * persisting.
         *
         * @param  path the path selecting zero or more values
         * @param  predicate the predicate that selects values to update
         * @param  updater the function that transforms each selected value
         * @param  <V> the focused value type
         * @return the committed configuration value
         */
        public <V> T updateWhereNoSave(ConfigPath.Many<T, V> path, Predicate<? super V> predicate, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(predicate);
            Objects.requireNonNull(updater);
            T current = unit.get();
            T updated = path.where(predicate).updateEach(current, updater);
            return unit.commitCandidate(current, updated, false);
        }
    }

    /**
     * A mutable accumulator that batches multiple field-level mutations
     * before committing them as a single change to a {@link ConfigUnit}.
     *
     * <p>Each mutation method returns {@code this} for fluent chaining.
     *
     * @param <T> the configuration type
     */
    public static final class BatchMutator<T> {
        private final ConfigOpticResolver<T> resolver;
        private T value;
        private boolean changed;

        private BatchMutator(T initialValue, ConfigOpticResolver<T> resolver) {
            this.value = initialValue;
            this.resolver = resolver;
        }

        /**
         * Returns the current accumulated value.
         *
         * @return the current value
         */
        public T value() {
            return value;
        }

        /**
         * Returns {@code true} if any mutation in this batch changed the value.
         *
         * @return {@code true} if changed
         */
        public boolean changed() {
            return changed;
        }

        /**
         * Returns operations that apply preconstructed paths to this batch.
         *
         * @return path-based operations for this batch
         */
        public PathBatchMutator<T> paths() {
            return new PathBatchMutator<>(this);
        }

        /**
         * Applies a getter-based update to the accumulated value.
         *
         * @param getter  a serializable method reference to a record component
         * @param updater a function transforming the current field value
         * @param <V>     the field value type
         * @return this mutator
         */
        public <V> BatchMutator<T> update(LensGetter<T, V> getter, UnaryOperator<V> updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            var lens = resolver.lens(getter);
            value = lens.modify(updater, value);
            changed = true;
            return this;
        }

        /**
         * Sets a field and returns the new field value.
         *
         * @param getter  a serializable method reference to a record component
         * @param newValue the new field value
         * @param <V>     the field value type
         * @return the new field value
         */
        public <V> V setAndGet(LensGetter<T, V> getter, V newValue) {
            Objects.requireNonNull(getter);
            var lens = resolver.lens(getter);
            value = lens.set(newValue, value);
            changed = true;
            return lens.get(value);
        }

        /**
         * Applies an integer update to the accumulated value.
         *
         * @param getter  a serializable method reference to an {@code int} record component
         * @param updater a function transforming the current value
         * @return this mutator
         */
        public BatchMutator<T> updateInt(LensGetter<T, Integer> getter, IntUnaryOperator updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            var lens = resolver.lens(getter);
            value = lens.modify(updater::applyAsInt, value);
            changed = true;
            return this;
        }

        /**
         * Applies a long update to the accumulated value.
         *
         * @param getter  a serializable method reference to a {@code long} record component
         * @param updater a function transforming the current value
         * @return this mutator
         */
        public BatchMutator<T> updateLong(LensGetter<T, Long> getter, LongUnaryOperator updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            var lens = resolver.lens(getter);
            value = lens.modify(updater::applyAsLong, value);
            changed = true;
            return this;
        }

        /**
         * Applies a double update to the accumulated value.
         *
         * @param getter  a serializable method reference to a {@code double} record component
         * @param updater a function transforming the current value
         * @return this mutator
         */
        public BatchMutator<T> updateDouble(LensGetter<T, Double> getter, DoubleUnaryOperator updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            var lens = resolver.lens(getter);
            value = lens.modify(updater::applyAsDouble, value);
            changed = true;
            return this;
        }

        /**
         * Applies a boolean update to the accumulated value.
         *
         * @param getter  a serializable method reference to a {@code boolean} record component
         * @param updater a function transforming the current value
         * @return this mutator
         */
        public BatchMutator<T> updateBoolean(LensGetter<T, Boolean> getter, BooleanUnaryOperator updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            var lens = resolver.lens(getter);
            value = lens.modify(updater::applyAsBoolean, value);
            changed = true;
            return this;
        }

        /**
         * Conditionally applies an update to an {@link Optional} field when it
         * has a present value.
         *
         * @param getter a serializable method reference to an {@code Optional} record component
         * @param updater a function transforming the present value
         * @param <V> the optional value type
         * @return this mutator
         */
        public <V> BatchMutator<T> ifPresent(LensGetter<T, Optional<V>> getter, UnaryOperator<V> updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            var selector = resolver.optional(getter);
            T next = selector.modify(updater, value);
            if (!Objects.equals(next, value)) {
                value = next;
                changed = true;
            }
            return this;
        }

        /**
         * Conditionally applies an update when a field's runtime value is an
         * instance of the specified subtype.
         *
         * @param getter a serializable method reference to a record component
         * @param subtype the expected subtype class
         * @param updater a function transforming the matched value
         * @param <V> the base field type
         * @param <X> the subtype to match
         * @return this mutator
         */
        public <V, X extends V> BatchMutator<T> whenSubtype(
                LensGetter<T, V> getter,
                Class<X> subtype,
                UnaryOperator<X> updater
        ) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(subtype);
            Objects.requireNonNull(updater);
            var selector = resolver.subtype(getter, subtype);
            T next = selector.modify(updater, value);
            if (!Objects.equals(next, value)) {
                value = next;
                changed = true;
            }
            return this;
        }

        /**
         * Transforms every element of a list-valued field.
         *
         * @param getter a serializable method reference to the list field accessor
         * @param modifier a function that transforms each list element
         * @param <V> the list element type
         * @return this mutator
         */
        public <V> BatchMutator<T> updateElements(LensGetter<T, List<V>> getter, UnaryOperator<V> modifier) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(modifier);
            var traversal = resolver.listTraversal(getter);
            value = traversal.modify(modifier, value);
            changed = true;
            return this;
        }

        /**
         * Transforms list elements that satisfy the given predicate.
         *
         * @param getter a serializable method reference to the list field accessor
         * @param predicate a predicate that selects which elements to transform
         * @param modifier a function that transforms each selected element
         * @param <V> the list element type
         * @return this mutator
         */
        public <V> BatchMutator<T> updateWhere(
                LensGetter<T, List<V>> getter,
                Predicate<V> predicate,
                UnaryOperator<V> modifier
        ) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(predicate);
            Objects.requireNonNull(modifier);
            var traversal = resolver.listTraversal(getter).filtered(predicate);
            value = traversal.modify(modifier, value);
            changed = true;
            return this;
        }

        /**
         * Transforms every value of a map-valued field.
         *
         * @param getter a serializable method reference to the map field accessor
         * @param modifier a function that transforms each map value
         * @param <K> the map key type
         * @param <V> the map value type
         * @return this mutator
         */
        public <K, V> BatchMutator<T> updateValues(LensGetter<T, Map<K, V>> getter, UnaryOperator<V> modifier) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(modifier);
            var traversal = resolver.mapValuesTraversal(getter);
            value = traversal.modify(modifier, value);
            changed = true;
            return this;
        }

        /**
         * Transforms map values that satisfy the given predicate.
         *
         * @param getter a serializable method reference to the map field accessor
         * @param predicate a predicate that selects which values to transform
         * @param modifier a function that transforms each selected value
         * @param <K> the map key type
         * @param <V> the map value type
         * @return this mutator
         */
        public <K, V> BatchMutator<T> updateValuesWhere(
                LensGetter<T, Map<K, V>> getter,
                Predicate<V> predicate,
                UnaryOperator<V> modifier
        ) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(predicate);
            Objects.requireNonNull(modifier);
            var traversal = resolver.mapValuesTraversal(getter).filtered(predicate);
            value = traversal.modify(modifier, value);
            changed = true;
            return this;
        }

    }

    /**
     * Performs path-based operations on a batch mutator.
     *
     * @param <T> the configuration type
     */
    public static final class PathBatchMutator<T> {
        private final BatchMutator<T> batch;

        private PathBatchMutator(BatchMutator<T> batch) {
            this.batch = Objects.requireNonNull(batch);
        }

        /**
         * Applies an update through an exactly-one path.
         *
         * @param  path the path selecting exactly one value
         * @param  updater the function that transforms the focused value
         * @param  <V> the focused value type
         * @return the owning batch mutator
         */
        public <V> BatchMutator<T> update(ConfigPath.One<T, V> path, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(updater);
            batch.value = path.update(batch.value, updater);
            batch.changed = true;
            return batch;
        }

        /**
         * Sets the value selected by an exactly-one path and returns the new
         * focused value.
         *
         * @param  path the path selecting exactly one value
         * @param  newValue the replacement value
         * @param  <V> the focused value type
         * @return the new focused value
         */
        public <V> V setAndGet(ConfigPath.One<T, V> path, V newValue) {
            Objects.requireNonNull(path);
            batch.value = path.set(batch.value, newValue);
            batch.changed = true;
            return path.view(batch.value);
        }

        /**
         * Applies an update through a zero-or-one path when it focuses a value.
         *
         * @param  path the path selecting zero or one value
         * @param  updater the function that transforms the focused value
         * @param  <V> the focused value type
         * @return the owning batch mutator
         */
        public <V> BatchMutator<T> ifPresent(ConfigPath.Maybe<T, V> path, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(updater);
            T next = path.updateIfPresent(batch.value, updater);
            if (!Objects.equals(next, batch.value)) {
                batch.value = next;
                batch.changed = true;
            }
            return batch;
        }

        /**
         * Applies an update through an exactly-one path when the focused value
         * has the specified runtime type.
         *
         * @param  path the path selecting exactly one value
         * @param  subtype the expected subtype class
         * @param  updater the function that transforms the matched value
         * @param  <V> the base focused value type
         * @param  <X> the subtype to match
         * @return the owning batch mutator
         */
        public <V, X extends V> BatchMutator<T> whenSubtype(ConfigPath.One<T, V> path, Class<X> subtype, UnaryOperator<X> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(subtype);
            Objects.requireNonNull(updater);
            V focused = path.view(batch.value);
            if (subtype.isInstance(focused)) {
                batch.value = path.set(batch.value, updater.apply(subtype.cast(focused)));
                batch.changed = true;
            }
            return batch;
        }

        /**
         * Applies an update through a zero-or-more path.
         *
         * @param  path the path selecting zero or more values
         * @param  updater the function that transforms each focused value
         * @param  <V> the focused value type
         * @return the owning batch mutator
         */
        public <V> BatchMutator<T> updateEach(ConfigPath.Many<T, V> path, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(updater);
            batch.value = path.updateEach(batch.value, updater);
            batch.changed = true;
            return batch;
        }

        /**
         * Updates focused values that satisfy the given predicate.
         *
         * @param  path the path selecting zero or more values
         * @param  predicate the predicate that selects values to update
         * @param  updater the function that transforms each selected value
         * @param  <V> the focused value type
         * @return the owning batch mutator
         */
        public <V> BatchMutator<T> updateWhere(ConfigPath.Many<T, V> path, Predicate<? super V> predicate, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(predicate);
            Objects.requireNonNull(updater);
            batch.value = path.where(predicate).updateEach(batch.value, updater);
            batch.changed = true;
            return batch;
        }
    }
}
