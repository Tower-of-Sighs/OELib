package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigPrism;
import cc.sighs.oelib.config.optics.internal.ConfigTraversal;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.*;

/**
 * Utility operations on {@link ConfigUnit} instances that offer finer
 * control over persistence and batching.
 *
 * <p>The "NoSave" variants (for example {@link #updateNoSave(ConfigUnit, RecordLensBuilder.LensGetter, UnaryOperator)})
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
     * Updates a field through a getter without persisting to disk.
     *
     * @param unit    the configuration unit
     * @param getter  a serializable method reference to a record component
     * @param updater a function transforming the current field value
     * @param <T>     the configuration type
     * @param <V>     the field value type
     * @return the committed configuration value
     */
    public static <T, V> T updateNoSave(ConfigUnit<T> unit, RecordLensBuilder.LensGetter<T, V> getter, UnaryOperator<V> updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        Class<T> rc = unit.recordClass();
        var lens = RecordLensBuilder.lens(rc, getter);
        T current = unit.get();
        T updated = lens.update(current, updater);
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
    public static <T, V> V setAndGet(ConfigUnit<T> unit, RecordLensBuilder.LensGetter<T, V> getter, V value) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Class<T> rc = unit.recordClass();
        var lens = RecordLensBuilder.lens(rc, getter);
        T current = unit.get();
        T updated = lens.set(current, value);
        T committed = unit.commitCandidate(current, updated, true);
        return lens.view(committed);
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
    public static <T, V> V setAndGetNoSave(ConfigUnit<T> unit, RecordLensBuilder.LensGetter<T, V> getter, V value) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Class<T> rc = unit.recordClass();
        var lens = RecordLensBuilder.lens(rc, getter);
        T current = unit.get();
        T updated = lens.set(current, value);
        T committed = unit.commitCandidate(current, updated, false);
        return lens.view(committed);
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
    public static <T> T updateIntNoSave(ConfigUnit<T> unit, RecordLensBuilder.LensGetter<T, Integer> getter, IntUnaryOperator updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        Class<T> rc = unit.recordClass();
        var lens = RecordLensBuilder.lens(rc, getter).asInt();
        T current = unit.get();
        T updated = lens.update(current, updater);
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
    public static <T> T updateLongNoSave(ConfigUnit<T> unit, RecordLensBuilder.LensGetter<T, Long> getter, LongUnaryOperator updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        Class<T> rc = unit.recordClass();
        var lens = RecordLensBuilder.lens(rc, getter).asLong();
        T current = unit.get();
        T updated = lens.update(current, updater);
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
    public static <T> T updateDoubleNoSave(ConfigUnit<T> unit, RecordLensBuilder.LensGetter<T, Double> getter, DoubleUnaryOperator updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        Class<T> rc = unit.recordClass();
        var lens = RecordLensBuilder.lens(rc, getter).asDouble();
        T current = unit.get();
        T updated = lens.update(current, updater);
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
    public static <T> T updateBooleanNoSave(ConfigUnit<T> unit, RecordLensBuilder.LensGetter<T, Boolean> getter, UnaryOperator<Boolean> updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(updater);
        Class<T> rc = unit.recordClass();
        var lens = RecordLensBuilder.lens(rc, getter).asBoolean();
        T current = unit.get();
        T updated = lens.raw().update(current, updater);
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
     * @throws NullPointerException if {@code unit} or {@code mutations} is {@code null}
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
            updated = mutation.apply(updated);
        }
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Conditionally updates a value through a prism without persisting.
     *
     * @param unit    the configuration unit
     * @param prism   the prism to match against
     * @param updater a function transforming the matched value
     * @param <T>     the configuration type
     * @param <V>     the matched value type
     * @return the committed (or unchanged) configuration value
     * @throws NullPointerException if any argument is {@code null}
     */
    public static <T, V> T ifPresentNoSave(ConfigUnit<T> unit, ConfigPrism<T, V> prism, UnaryOperator<V> updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(prism);
        Objects.requireNonNull(updater);
        T current = unit.get();
        T updated = prism.updateIfPresent(current, updater);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Alias for {@link #ifPresentNoSave(ConfigUnit, ConfigPrism, UnaryOperator)}.
     *
     * @param unit    the configuration unit
     * @param prism   the prism to match against
     * @param updater a function transforming the matched value
     * @param <T>     the configuration type
     * @param <V>     the matched value type
     * @return the committed (or unchanged) configuration value
     */
    public static <T, V> T whenSubtypeNoSave(ConfigUnit<T> unit, ConfigPrism<T, V> prism, UnaryOperator<V> updater) {
        return ifPresentNoSave(unit, prism, updater);
    }

    /**
     * Applies a traversal update without persisting to disk.
     *
     * @param unit      the configuration unit
     * @param traversal the traversal that selects which elements to focus
     * @param modifier  a function that transforms each focused element
     * @param <T>       the configuration type
     * @param <V>       the element type
     */
    @ApiStatus.Internal
    public static <T, V> void traverseNoSave(ConfigUnit<T> unit, ConfigTraversal<T, V> traversal, UnaryOperator<V> modifier) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(traversal);
        Objects.requireNonNull(modifier);
        T current = unit.get();
        T updated = traversal.update(current, modifier);
        unit.commitCandidate(current, updated, false);
    }

    /**
     * Executes a batch of mutations through a {@link BatchMutator}, persists
     * if any change was made, and returns the final value.
     *
     * @param unit        the configuration unit
     * @param batchAction a consumer that populates the batch mutator
     * @param <T>         the configuration type
     * @return the final configuration value after the batch
     * @throws NullPointerException if any argument is {@code null}
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
     * @throws NullPointerException if any argument is {@code null}
     */
    public static <T> BatchMutator<T> withBatchNoSave(ConfigUnit<T> unit, Consumer<BatchMutator<T>> batchAction) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(batchAction);
        T current = unit.get();
        BatchMutator<T> batch = new BatchMutator<>(current);
        batchAction.accept(batch);
        if (batch.changed()) {
            unit.commitCandidate(current, batch.value(), false);
        }
        return batch;
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
        private T value;
        private boolean changed;

        private BatchMutator(T initialValue) {
            this.value = initialValue;
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
         * Applies a getter-based update to the accumulated value.
         *
         * @param getter  a serializable method reference to a record component
         * @param updater a function transforming the current field value
         * @param <V>     the field value type
         * @return this mutator
         */
        public <V> BatchMutator<T> update(RecordLensBuilder.LensGetter<T, V> getter, UnaryOperator<V> updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            Class<T> rc = rc();
            var lens = RecordLensBuilder.lens(rc, getter);
            value = lens.update(value, updater);
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
        public <V> V setAndGet(RecordLensBuilder.LensGetter<T, V> getter, V newValue) {
            Objects.requireNonNull(getter);
            Class<T> rc = rc();
            var lens = RecordLensBuilder.lens(rc, getter);
            value = lens.set(value, newValue);
            changed = true;
            return lens.view(value);
        }

        /**
         * Applies an integer update to the accumulated value.
         *
         * @param getter  a serializable method reference to an {@code int} record component
         * @param updater a function transforming the current value
         * @return this mutator
         */
        public BatchMutator<T> updateInt(RecordLensBuilder.LensGetter<T, Integer> getter, IntUnaryOperator updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            Class<T> rc = rc();
            var lens = RecordLensBuilder.lens(rc, getter).asInt();
            value = lens.update(value, updater);
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
        public BatchMutator<T> updateLong(RecordLensBuilder.LensGetter<T, Long> getter, LongUnaryOperator updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            Class<T> rc = rc();
            var lens = RecordLensBuilder.lens(rc, getter).asLong();
            value = lens.update(value, updater);
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
        public BatchMutator<T> updateDouble(RecordLensBuilder.LensGetter<T, Double> getter, DoubleUnaryOperator updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            Class<T> rc = rc();
            var lens = RecordLensBuilder.lens(rc, getter).asDouble();
            value = lens.update(value, updater);
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
        public BatchMutator<T> updateBoolean(RecordLensBuilder.LensGetter<T, Boolean> getter, UnaryOperator<Boolean> updater) {
            Objects.requireNonNull(getter);
            Objects.requireNonNull(updater);
            Class<T> rc = rc();
            var lens = RecordLensBuilder.lens(rc, getter).asBoolean();
            value = lens.raw().update(value, updater);
            changed = true;
            return this;
        }

        /**
         * Conditionally applies an update through a prism.
         *
         * @param prism   the prism to match against
         * @param updater a function transforming the matched value
         * @param <V>     the matched value type
         * @return this mutator
         */
        public <V> BatchMutator<T> ifPresent(ConfigPrism<T, V> prism, UnaryOperator<V> updater) {
            Objects.requireNonNull(prism);
            Objects.requireNonNull(updater);
            T next = prism.updateIfPresent(value, updater);
            if (!Objects.equals(next, value)) {
                value = next;
                changed = true;
            }
            return this;
        }

        /**
         * Applies a traversal update to the accumulated value.
         *
         * @param traversal the traversal that selects which elements to focus
         * @param modifier  a function that transforms each focused element
         * @param <V>       the element type
         */
        @ApiStatus.Internal
        public <V> void traverse(ConfigTraversal<T, V> traversal, UnaryOperator<V> modifier) {
            Objects.requireNonNull(traversal);
            Objects.requireNonNull(modifier);
            value = traversal.update(value, modifier);
            changed = true;
        }

        @SuppressWarnings("unchecked")
        private Class<T> rc() {
            return (Class<T>) value.getClass();
        }
    }
}
