package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigAffine;
import cc.sighs.oelib.config.optics.ConfigTraversal;
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
        var lens = unit.resolver().lens(getter);
        T current = unit.get();
        T updated = lens.update(current, updater);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Updates a single value through a path without persisting to disk.
     *
     * @param  unit the configuration unit
     * @param  path the path selecting exactly one value
     * @param  updater the function that transforms the focused value
     * @param  <T> the configuration type
     * @param  <V> the focused value type
     * @return the committed configuration value
     */
    public static <T, V> T updateNoSave(ConfigUnit<T> unit, ConfigPath.One<T, V> path, UnaryOperator<V> updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(path);
        Objects.requireNonNull(updater);
        T current = unit.get();
        T updated = path.update(current, updater);
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
        var lens = unit.resolver().lens(getter);
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
        var lens = unit.resolver().lens(getter);
        T current = unit.get();
        T updated = lens.set(current, value);
        T committed = unit.commitCandidate(current, updated, false);
        return lens.view(committed);
    }

    /**
     * Sets a single value through a path without persisting and returns the
     * committed focused value.
     *
     * @param  unit the configuration unit
     * @param  path the path selecting exactly one value
     * @param  value the replacement value
     * @param  <T> the configuration type
     * @param  <V> the focused value type
     * @return the committed focused value
     */
    public static <T, V> V setAndGetNoSave(ConfigUnit<T> unit, ConfigPath.One<T, V> path, V value) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(path);
        T current = unit.get();
        T updated = path.set(current, value);
        T committed = unit.commitCandidate(current, updated, false);
        return path.view(committed);
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
        var lens = unit.resolver().lens(getter).asInt();
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
        var lens = unit.resolver().lens(getter).asLong();
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
        var lens = unit.resolver().lens(getter).asDouble();
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
        var lens = unit.resolver().lens(getter).asBoolean();
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
            if (mutation instanceof ContextualMutation<T> contextual) {
                updated = contextual.apply(updated, unit.resolver());
            } else {
                updated = mutation.apply(updated);
            }
        }
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Conditionally updates a value through an affine without persisting.
     *
     * @param unit    the configuration unit
     * @param affine   the affine to match against
     * @param updater a function transforming the matched value
     * @param <T>     the configuration type
     * @param <V>     the matched value type
     * @return the committed (or unchanged) configuration value
     * @throws NullPointerException if any argument is {@code null}
     */
    public static <T, V> T ifPresentNoSave(ConfigUnit<T> unit, ConfigAffine<T, V> affine, UnaryOperator<V> updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(affine);
        Objects.requireNonNull(updater);
        T current = unit.get();
        T updated = affine.updateIfPresent(current, updater);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Updates the focused value selected by the given zero-or-one path when it
     * is present, without persisting to disk.
     *
     * @param  unit the configuration unit
     * @param  path the path selecting zero or one value
     * @param  updater the function that transforms the focused value
     * @param  <T> the configuration type
     * @param  <V> the focused value type
     * @return the committed configuration value
     */
    public static <T, V> T ifPresentNoSave(ConfigUnit<T> unit, ConfigPath.Maybe<T, V> path, UnaryOperator<V> updater) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(path);
        Objects.requireNonNull(updater);
        T current = unit.get();
        T updated = path.updateIfPresent(current, updater);
        return unit.commitCandidate(current, updated, false);
    }

    /**
     * Alias for {@link #ifPresentNoSave(ConfigUnit, ConfigAffine, UnaryOperator)}.
     *
     * @param unit    the configuration unit
     * @param affine   the affine to match against
     * @param updater a function transforming the matched value
     * @param <T>     the configuration type
     * @param <V>     the matched value type
     * @return the committed (or unchanged) configuration value
     */
    public static <T, V> T whenSubtypeNoSave(ConfigUnit<T> unit, ConfigAffine<T, V> affine, UnaryOperator<V> updater) {
        return ifPresentNoSave(unit, affine, updater);
    }

    /**
     * Alias for {@link #ifPresentNoSave(ConfigUnit, ConfigPath.Maybe, UnaryOperator)}.
     *
     * @param  unit the configuration unit
     * @param  path the path selecting zero or one value
     * @param  updater the function that transforms the focused value
     * @param  <T> the configuration type
     * @param  <V> the focused value type
     * @return the committed configuration value
     */
    public static <T, V> T whenSubtypeNoSave(ConfigUnit<T> unit, ConfigPath.Maybe<T, V> path, UnaryOperator<V> updater) {
        return ifPresentNoSave(unit, path, updater);
    }

    /**
     * Updates all values selected by the given path without persisting to disk.
     *
     * @param  unit the configuration unit
     * @param  path the path selecting zero or more values
     * @param  modifier the function that transforms each focused value
     * @param  <T> the configuration type
     * @param  <V> the focused value type
     * @return the committed configuration value
     */
    public static <T, V> T updateEachNoSave(ConfigUnit<T> unit, ConfigPath.Many<T, V> path, UnaryOperator<V> modifier) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(path);
        Objects.requireNonNull(modifier);
        T current = unit.get();
        T updated = path.updateEach(current, modifier);
        return unit.commitCandidate(current, updated, false);
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
        BatchMutator<T> batch = new BatchMutator<>(current, unit.resolver());
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
            var lens = resolver.lens(getter);
            value = lens.update(value, updater);
            changed = true;
            return this;
        }

        /**
         * Applies an update through a path selecting exactly one value.
         *
         * @param  path the path selecting exactly one value
         * @param  updater the function that transforms the focused value
         * @param  <V> the focused value type
         * @return this mutator
         */
        public <V> BatchMutator<T> update(ConfigPath.One<T, V> path, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(updater);
            value = path.update(value, updater);
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
            var lens = resolver.lens(getter);
            value = lens.set(value, newValue);
            changed = true;
            return lens.view(value);
        }

        /**
         * Sets the value selected by the given path and returns the new focused
         * value.
         *
         * @param  path the path selecting exactly one value
         * @param  newValue the replacement value
         * @param  <V> the focused value type
         * @return the new focused value
         */
        public <V> V setAndGet(ConfigPath.One<T, V> path, V newValue) {
            Objects.requireNonNull(path);
            value = path.set(value, newValue);
            changed = true;
            return path.view(value);
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
            var lens = resolver.lens(getter).asInt();
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
            var lens = resolver.lens(getter).asLong();
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
            var lens = resolver.lens(getter).asDouble();
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
            var lens = resolver.lens(getter).asBoolean();
            value = lens.raw().update(value, updater);
            changed = true;
            return this;
        }

        /**
         * Conditionally applies an update through an affine.
         *
         * @param affine   the affine to match against
         * @param updater a function transforming the matched value
         * @param <V>     the matched value type
         * @return this mutator
         */
        @ApiStatus.Experimental
        public <V> BatchMutator<T> ifPresent(ConfigAffine<T, V> affine, UnaryOperator<V> updater) {
            Objects.requireNonNull(affine);
            Objects.requireNonNull(updater);
            T next = affine.updateIfPresent(value, updater);
            if (!Objects.equals(next, value)) {
                value = next;
                changed = true;
            }
            return this;
        }

        /**
         * Applies an update through a zero-or-one path when it focuses a value.
         *
         * @param  path the path selecting zero or one value
         * @param  updater the function that transforms the focused value
         * @param  <V> the focused value type
         * @return this mutator
         */
        public <V> BatchMutator<T> ifPresent(ConfigPath.Maybe<T, V> path, UnaryOperator<V> updater) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(updater);
            T next = path.updateIfPresent(value, updater);
            if (!Objects.equals(next, value)) {
                value = next;
                changed = true;
            }
            return this;
        }

        /**
         * Applies an update through a path selecting zero or more values.
         *
         * @param  path the path selecting zero or more values
         * @param  modifier the function that transforms each focused value
         * @param  <V> the focused value type
         * @return this mutator
         */
        public <V> BatchMutator<T> updateEach(ConfigPath.Many<T, V> path, UnaryOperator<V> modifier) {
            Objects.requireNonNull(path);
            Objects.requireNonNull(modifier);
            value = path.updateEach(value, modifier);
            changed = true;
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
    }
}
