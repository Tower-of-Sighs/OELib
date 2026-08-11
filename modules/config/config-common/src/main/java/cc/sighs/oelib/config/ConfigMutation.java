package cc.sighs.oelib.config;

import com.flechazo.optics.Lens;
import com.flechazo.optics.LensGetter;
import com.flechazo.optics.Optic;
import com.flechazo.optics.OpticBatch;
import com.flechazo.optics.util.Traversals;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Represents an immutable, ordered configuration mutation.
 *
 * <p>Each append operation returns a new mutation and leaves the original unchanged. Applying the
 * mutation preserves the observable order of transformations, predicates, and exceptions.
 *
 * @param <T> the configuration root type
 */
public final class ConfigMutation<T> implements UnaryOperator<T> {
    private final Class<T> rootClass;
    private final List<Edit<T>> edits;

    ConfigMutation(Class<T> rootClass) {
        this(rootClass, List.of());
    }

    private ConfigMutation(Class<T> rootClass, List<Edit<T>> edits) {
        this.rootClass = Objects.requireNonNull(rootClass, "rootClass");
        this.edits = List.copyOf(edits);
    }

    /**
     * Returns the configuration root type accepted by this mutation.
     *
     * @return the configuration root type
     */
    public Class<T> rootClass() {
        return rootClass;
    }

    /**
     * Applies the appended transformations to a configuration value.
     *
     * @param source the configuration value to transform
     * @return the transformed configuration value
     */
    @Override
    public T apply(T source) {
        return toBatch().apply(source);
    }

    /**
     * Returns a mutation that applies the specified root transformation after
     * the operations in this mutation.
     *
     * @param next the root transformation to append
     * @return a mutation containing the existing operations followed by
     *         {@code next}
     */
    public ConfigMutation<T> then(UnaryOperator<T> next) {
        Objects.requireNonNull(next, "next");
        return append(next);
    }

    /**
     * Returns a mutation that replaces the selected record component.
     *
     * @param getter the accessor identifying the record component
     * @param value the replacement value
     * @param <A> the component type
     * @return a mutation containing the replacement operation
     */
    public <A> ConfigMutation<T> set(LensGetter<T, A> getter, A value) {
        Objects.requireNonNull(getter, "getter");
        Lens<T, A> lens = RecordLensBuilder.lens(rootClass, getter);
        return appendSet(lens, value);
    }

    /**
     * Returns a mutation that transforms the selected record component.
     *
     * @param getter the accessor identifying the record component
     * @param modifier the component transformation
     * @param <A> the component type
     * @return a mutation containing the component transformation
     */
    public <A> ConfigMutation<T> map(
            LensGetter<T, A> getter, UnaryOperator<A> modifier) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(modifier, "modifier");
        Lens<T, A> lens = RecordLensBuilder.lens(rootClass, getter);
        return appendModify(lens, modifier);
    }

    /**
     * Returns a mutation that transforms the value of a nonempty
     * {@link Optional} component.
     *
     * <p>An empty component remains empty.
     *
     * @param getter the accessor identifying the optional component
     * @param modifier the transformation applied to a present value
     * @param <A> the optional value type
     * @return a mutation containing the conditional transformation
     */
    public <A> ConfigMutation<T> ifPresent(
            LensGetter<T, Optional<A>> getter, UnaryOperator<A> modifier) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(modifier, "modifier");
        var affine = RecordLensBuilder.optional(RecordLensBuilder.lens(rootClass, getter));
        return appendModify(affine, modifier);
    }

    /**
     * Returns a mutation that transforms a component when its value is an
     * instance of the specified subtype.
     *
     * <p>A value of another runtime type remains unchanged.
     *
     * @param getter the accessor identifying the component
     * @param subtypeClass the subtype accepted by the transformation
     * @param modifier the transformation applied to a matching value
     * @param <A> the component base type
     * @param <X> the selected subtype
     * @return a mutation containing the conditional transformation
     */
    public <A, X extends A> ConfigMutation<T> whenSubtype(
            LensGetter<T, A> getter,
            Class<X> subtypeClass,
            UnaryOperator<X> modifier) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(subtypeClass, "subtypeClass");
        Objects.requireNonNull(modifier, "modifier");
        var affine = RecordLensBuilder.subtype(
                RecordLensBuilder.lens(rootClass, getter), subtypeClass);
        return appendModify(affine, modifier);
    }

    /**
     * Returns a mutation that transforms every element of a {@link List}
     * component in encounter order.
     *
     * @param getter the accessor identifying the list component
     * @param modifier the transformation applied to each element
     * @param <A> the element type
     * @return a mutation containing the element transformations
     */
    public <A> ConfigMutation<T> updateElements(
            LensGetter<T, List<A>> getter, UnaryOperator<A> modifier) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(modifier, "modifier");
        var traversal = RecordLensBuilder.lens(rootClass, getter)
                .andThen(Traversals.<A>forList());
        return appendModify(traversal, modifier);
    }

    /**
     * Returns a mutation that transforms list elements satisfying a predicate.
     *
     * <p>Elements that do not satisfy the predicate remain unchanged.
     *
     * @param getter the accessor identifying the list component
     * @param predicate the condition selecting elements to transform
     * @param modifier the transformation applied to selected elements
     * @param <A> the element type
     * @return a mutation containing the selected element transformations
     */
    public <A> ConfigMutation<T> updateWhere(
            LensGetter<T, List<A>> getter,
            Predicate<? super A> predicate,
            UnaryOperator<A> modifier) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(modifier, "modifier");
        var traversal = RecordLensBuilder.lens(rootClass, getter)
                .andThen(Traversals.<A>forList())
                .filtered(predicate);
        return appendModify(traversal, modifier);
    }

    /**
     * Returns a mutation that transforms every value of a {@link Map}
     * component while preserving its keys.
     *
     * @param getter the accessor identifying the map component
     * @param modifier the transformation applied to each value
     * @param <K> the map key type
     * @param <V> the map value type
     * @return a mutation containing the value transformations
     */
    public <K, V> ConfigMutation<T> updateValues(
            LensGetter<T, Map<K, V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(modifier, "modifier");
        var traversal = RecordLensBuilder.lens(rootClass, getter)
                .andThen(Traversals.<K, V>forMapValues());
        return appendModify(traversal, modifier);
    }

    /**
     * Returns a mutation that transforms map values satisfying a predicate.
     *
     * <p>Keys and values that do not satisfy the predicate remain unchanged.
     *
     * @param getter the accessor identifying the map component
     * @param predicate the condition selecting values to transform
     * @param modifier the transformation applied to selected values
     * @param <K> the map key type
     * @param <V> the map value type
     * @return a mutation containing the selected value transformations
     */
    public <K, V> ConfigMutation<T> updateValuesWhere(
            LensGetter<T, Map<K, V>> getter,
            Predicate<? super V> predicate,
            UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(modifier, "modifier");
        var traversal = RecordLensBuilder.lens(rootClass, getter)
                .andThen(Traversals.<K, V>forMapValues())
                .filtered(predicate);
        return appendModify(traversal, modifier);
    }

    /**
     * Returns a mutation that replaces the value selected by an exactly-one
     * focus.
     *
     * @param focus the focus selecting the value to replace
     * @param value the replacement value
     * @param <A> the focused value type
     * @return a mutation containing the replacement operation
     */
    public <A> ConfigMutation<T> set(ConfigFocus.One<T, A> focus, A value) {
        Objects.requireNonNull(focus, "focus");
        return appendSet(focus.prototype().toLens(), value);
    }

    /**
     * Returns a mutation that transforms the value selected by an exactly-one
     * focus.
     *
     * @param focus the focus selecting the value to transform
     * @param modifier the focused value transformation
     * @param <A> the focused value type
     * @return a mutation containing the focused transformation
     */
    public <A> ConfigMutation<T> map(
            ConfigFocus.One<T, A> focus, UnaryOperator<A> modifier) {
        Objects.requireNonNull(focus, "focus");
        Objects.requireNonNull(modifier, "modifier");
        return appendModify(focus.prototype().toLens(), modifier);
    }

    /**
     * Returns a mutation that transforms the value selected by a zero-or-one
     * focus when that value is present.
     *
     * @param focus the optional focus selecting the value
     * @param modifier the transformation applied to a present value
     * @param <A> the focused value type
     * @return a mutation containing the conditional transformation
     */
    public <A> ConfigMutation<T> ifPresent(
            ConfigFocus.Maybe<T, A> focus, UnaryOperator<A> modifier) {
        Objects.requireNonNull(focus, "focus");
        Objects.requireNonNull(modifier, "modifier");
        return appendModify(focus.prototype().toAffine(), modifier);
    }

    /**
     * Returns a mutation that transforms every value selected by a multi-focus.
     *
     * @param focus the focus selecting values to transform
     * @param modifier the transformation applied to each selected value
     * @param <A> the focused value type
     * @return a mutation containing the focused transformations
     */
    public <A> ConfigMutation<T> updateEach(
            ConfigFocus.Many<T, A> focus, UnaryOperator<A> modifier) {
        Objects.requireNonNull(focus, "focus");
        Objects.requireNonNull(modifier, "modifier");
        return appendModify(focus.prototype().toTraversal(), modifier);
    }

    /**
     * Returns a mutation that transforms selected values satisfying a
     * predicate.
     *
     * <p>Selected values that do not satisfy the predicate remain unchanged.
     *
     * @param focus the focus selecting candidate values
     * @param predicate the condition selecting values to transform
     * @param modifier the transformation applied to matching values
     * @param <A> the focused value type
     * @return a mutation containing the conditional transformations
     */
    public <A> ConfigMutation<T> updateWhere(
            ConfigFocus.Many<T, A> focus,
            Predicate<? super A> predicate,
            UnaryOperator<A> modifier) {
        Objects.requireNonNull(predicate, "predicate");
        return updateEach(focus.filter(predicate), modifier);
    }

    private ConfigMutation<T> append(UnaryOperator<T> next) {
        return appendEdit(new OpaqueEdit<>(next));
    }

    private <A> ConfigMutation<T> appendSet(Optic<T, T, A, A> optic, A value) {
        return appendEdit(new SetEdit<>(optic, value));
    }

    private <A> ConfigMutation<T> appendModify(
            Optic<T, T, A, A> optic, UnaryOperator<A> modifier) {
        return appendEdit(new ModifyEdit<>(optic, modifier));
    }

    private ConfigMutation<T> appendEdit(Edit<T> edit) {
        ArrayList<Edit<T>> next = new ArrayList<>(edits.size() + 1);
        next.addAll(edits);
        next.add(edit);
        return new ConfigMutation<>(rootClass, next);
    }

    private OpticBatch<T> toBatch() {
        OpticBatch<T> batch = OpticBatch.empty();
        for (Edit<T> edit : edits) {
            batch = edit.appendTo(batch);
        }
        return batch;
    }

    private sealed interface Edit<S> permits SetEdit, ModifyEdit, OpaqueEdit {
        OpticBatch<S> appendTo(OpticBatch<S> batch);
    }

    private record SetEdit<S, A>(Optic<S, S, A, A> optic, A value) implements Edit<S> {
        private SetEdit {
            Objects.requireNonNull(optic, "optic");
            Objects.requireNonNull(value, "value");
        }

        @Override
        public OpticBatch<S> appendTo(OpticBatch<S> batch) {
            return batch.set(optic, value);
        }

    }

    private record ModifyEdit<S, A>(Optic<S, S, A, A> optic, UnaryOperator<A> modifier)
            implements Edit<S> {
        private ModifyEdit {
            Objects.requireNonNull(optic, "optic");
            Objects.requireNonNull(modifier, "modifier");
        }

        @Override
        public OpticBatch<S> appendTo(OpticBatch<S> batch) {
            return batch.modify(optic, modifier);
        }

    }

    private record OpaqueEdit<S>(UnaryOperator<S> operation) implements Edit<S> {
        private OpaqueEdit {
            Objects.requireNonNull(operation, "operation");
        }

        @Override
        public OpticBatch<S> appendTo(OpticBatch<S> batch) {
            return batch.thenOpaque(operation);
        }

    }
}
