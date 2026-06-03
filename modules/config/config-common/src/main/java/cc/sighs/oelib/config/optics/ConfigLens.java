package cc.sighs.oelib.config.optics;

import cc.sighs.oelib.config.ConfigMutation;
import cc.sighs.oelib.config.RecordLensBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.*;

/**
 * A composable, bidirectional accessor for a field within a configuration record.
 *
 * <p>An {@code ConfigLens} combines a view operation (reading a field from a
 * record) with an update operation (producing a new record with the field
 * replaced). Lenses are immutable and can be composed via
 * {@link #compose(ConfigLens)} to traverse nested records.
 *
 * <p>Type-specialized wrappers are available through {@link #asInt()},
 * {@link #asLong()}, {@link #asDouble()}, and {@link #asBoolean()}.
 * Mutations can be lifted into {@link ConfigMutation} values via
 * {@link #map(UnaryOperator)} and {@link #setTo(Object)}.
 *
 * @param <S> the source (record) type
 * @param <A> the field (component) type
 */
public final class ConfigLens<S, A> {
    private final String path;
    private final Function<S, A> viewFn;
    private final BiFunction<A, S, S> setFn;
    private final RecordLensPlan plan;

    /**
     * Constructs a lens with an optional record plan for optimized composition.
     *
     * @param path   the dotted path to this field
     * @param viewFn a function that extracts the field value from the source
     * @param setFn  a function that produces a new source with the field replaced
     * @param plan   the record lens plan, or {@code null}
     */
    public ConfigLens(String path, Function<S, A> viewFn, BiFunction<A, S, S> setFn, @Nullable RecordLensPlan plan) {
        this.path = Objects.requireNonNull(path);
        this.viewFn = Objects.requireNonNull(viewFn);
        this.setFn = Objects.requireNonNull(setFn);
        this.plan = plan;
    }

    /**
     * Creates a root-level lens plan for a single component.
     *
     * @param lookup        the lookup used for access
     * @param rootClass     the root record class
     * @param leafClass     the component type
     * @param componentName the component name
     * @return a new plan
     */
    public static RecordLensPlan rootPlan(MethodHandles.Lookup lookup, Class<?> rootClass, Class<?> leafClass, String componentName) {
        return new RecordLensPlan(lookup, rootClass, leafClass, List.of(componentName));
    }

    /**
     * Returns the dotted path to this field.
     *
     * @return the path
     */
    public String path() {
        return path;
    }

    /**
     * Reads the field value from the given source.
     *
     * @param source the source record
     * @return the field value
     */
    public A view(S source) {
        return viewFn.apply(source);
    }

    /**
     * Produces a new record with this field set to the given value.
     *
     * @param source the source record
     * @param value  the new field value
     * @return a new record with the field replaced
     */
    public S set(S source, A value) {
        return setFn.apply(value, source);
    }

    /**
     * Produces a new record with this field transformed by the given function.
     *
     * @param source  the source record
     * @param updater a function that receives the current field value and
     *                returns the new value
     * @return a new record with the field updated
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public S update(S source, UnaryOperator<A> updater) {
        Objects.requireNonNull(updater);
        return setFn.apply(updater.apply(viewFn.apply(source)), source);
    }

    /**
     * Updates an integer-typed field through a primitive operator.
     *
     * @param source  the source record
     * @param updater a function transforming the current {@code int} value
     * @return a new record with the field updated
     * @throws NullPointerException  if {@code updater} is {@code null}
     * @throws ClassCastException    if the field is not an {@link Integer}
     */
    public S updateInt(S source, IntUnaryOperator updater) {
        Objects.requireNonNull(updater);
        Integer value = (Integer) viewFn.apply(source);
        @SuppressWarnings("unchecked")
        A after = (A) Integer.valueOf(updater.applyAsInt(value));
        return setFn.apply(after, source);
    }

    /**
     * Updates a long-typed field through a primitive operator.
     *
     * @param source  the source record
     * @param updater a function transforming the current {@code long} value
     * @return a new record with the field updated
     * @throws NullPointerException  if {@code updater} is {@code null}
     * @throws ClassCastException    if the field is not a {@link Long}
     */
    public S updateLong(S source, LongUnaryOperator updater) {
        Objects.requireNonNull(updater);
        Long value = (Long) viewFn.apply(source);
        @SuppressWarnings("unchecked")
        A after = (A) Long.valueOf(updater.applyAsLong(value));
        return setFn.apply(after, source);
    }

    /**
     * Updates a double-typed field through a primitive operator.
     *
     * @param source  the source record
     * @param updater a function transforming the current {@code double} value
     * @return a new record with the field updated
     * @throws NullPointerException  if {@code updater} is {@code null}
     * @throws ClassCastException    if the field is not a {@link Double}
     */
    public S updateDouble(S source, DoubleUnaryOperator updater) {
        Objects.requireNonNull(updater);
        Double value = (Double) viewFn.apply(source);
        @SuppressWarnings("unchecked")
        A after = (A) Double.valueOf(updater.applyAsDouble(value));
        return setFn.apply(after, source);
    }

    /**
     * Updates a boolean-typed field through a primitive operator.
     *
     * @param source  the source record
     * @param updater a function transforming the current {@code boolean} value
     * @return a new record with the field updated
     * @throws NullPointerException  if {@code updater} is {@code null}
     * @throws ClassCastException    if the field is not a {@link Boolean}
     */
    public S updateBoolean(S source, BooleanUnaryOperator updater) {
        Objects.requireNonNull(updater);
        Boolean value = (Boolean) viewFn.apply(source);
        @SuppressWarnings("unchecked")
        A after = (A) Boolean.valueOf(updater.applyAsBoolean(value));
        return setFn.apply(after, source);
    }

    /**
     * Returns an integer-specialized wrapper around this lens.
     *
     * @return an {@link ConfigIntLens}
     * @throws ClassCastException if the field is not an {@link Integer}
     */
    public ConfigIntLens<S> asInt() {
        @SuppressWarnings("unchecked")
        ConfigLens<S, Integer> typed = (ConfigLens<S, Integer>) this;
        return ConfigIntLens.of(typed);
    }

    /**
     * Returns a long-specialized wrapper around this lens.
     *
     * @return an {@link ConfigLongLens}
     * @throws ClassCastException if the field is not a {@link Long}
     */
    public ConfigLongLens<S> asLong() {
        @SuppressWarnings("unchecked")
        ConfigLens<S, Long> typed = (ConfigLens<S, Long>) this;
        return ConfigLongLens.of(typed);
    }

    /**
     * Returns a double-specialized wrapper around this lens.
     *
     * @return an {@link ConfigDoubleLens}
     * @throws ClassCastException if the field is not a {@link Double}
     */
    public ConfigDoubleLens<S> asDouble() {
        @SuppressWarnings("unchecked")
        ConfigLens<S, Double> typed = (ConfigLens<S, Double>) this;
        return ConfigDoubleLens.of(typed);
    }

    /**
     * Returns a boolean-specialized wrapper around this lens.
     *
     * @return an {@link ConfigBooleanLens}
     * @throws ClassCastException if the field is not a {@link Boolean}
     */
    public ConfigBooleanLens<S> asBoolean() {
        @SuppressWarnings("unchecked")
        ConfigLens<S, Boolean> typed = (ConfigLens<S, Boolean>) this;
        return ConfigBooleanLens.of(typed);
    }

    /**
     * Returns a mutation that sets this field to a constant value.
     *
     * @param value the value to set
     * @return a mutation
     */
    public ConfigMutation<S> setTo(A value) {
        return source -> set(source, value);
    }

    /**
     * Returns a mutation that transforms this field through the given function.
     *
     * @param updater a function transforming the current field value
     * @return a mutation
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public ConfigMutation<S> map(UnaryOperator<A> updater) {
        Objects.requireNonNull(updater);
        return source -> update(source, updater);
    }

    /**
     * Returns a mutation that transforms this integer field.
     *
     * @param updater a function transforming the current value
     * @return a mutation
     */
    public ConfigMutation<S> mapInt(IntUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return source -> updateInt(source, updater);
    }

    /**
     * Returns a mutation that transforms this long field.
     *
     * @param updater a function transforming the current value
     * @return a mutation
     */
    public ConfigMutation<S> mapLong(LongUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return source -> updateLong(source, updater);
    }

    /**
     * Returns a mutation that transforms this double field.
     *
     * @param updater a function transforming the current value
     * @return a mutation
     */
    public ConfigMutation<S> mapDouble(DoubleUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return source -> updateDouble(source, updater);
    }

    /**
     * Returns a mutation that transforms this boolean field.
     *
     * @param updater a function transforming the current value
     * @return a mutation
     */
    public ConfigMutation<S> mapBoolean(BooleanUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return source -> updateBoolean(source, updater);
    }

    /**
     * Composes this lens with a child lens, producing a lens that traverses
     * into a nested field.
     *
     * <p>If both lenses carry a {@link RecordLensPlan}, the composition is
     * resolved through {@link RecordLensBuilder#lensByPath} for optimal
     * generated-code performance. Otherwise a generic composed lens is used.
     *
     * @param child the lens targeting a field within this lens's value type
     * @param <B>   the child field type
     * @return a composed lens
     * @throws NullPointerException if {@code child} is {@code null}
     */
    public <B> ConfigLens<S, B> compose(ConfigLens<A, B> child) {
        Objects.requireNonNull(child);
        if (this.plan != null && child.plan != null) {
            RecordLensPlan merged = this.plan.compose(child.plan);
            if (merged != null) {
                return RecordLensBuilder.lensByPath(
                        merged.lookup(),
                        merged.rootClass(),
                        merged.path(),
                        merged.leafClass()
                );
            }
        }
        String composedPath = path.isBlank() ? child.path : path + "." + child.path;
        return new ConfigLens<>(
                composedPath,
                source -> child.view(view(source)),
                (value, source) -> set(source, child.set(view(source), value)),
                null
        );
    }

    /**
     * Returns the record lens plan, or {@code null} if this lens was not
     * created through {@link RecordLensBuilder}.
     *
     * @return the plan, or {@code null}
     */
    @Nullable
    public RecordLensPlan plan() {
        return plan;
    }

    /**
     * A primitive specialisation of {@link UnaryOperator} for {@code boolean}
     * values, avoiding the boxing overhead of {@code UnaryOperator<Boolean>}.
     */
    @FunctionalInterface
    public interface BooleanUnaryOperator {
        /**
         * Applies this operator to the given boolean value.
         *
         * @param value the input value
         * @return the result
         */
        boolean applyAsBoolean(boolean value);
    }

    /**
     * Metadata for a lens created from a record class, enabling optimized
     * composition through {@link RecordLensBuilder#lensByPath}.
     *
     * @param lookup    the lookup used for access control
     * @param rootClass the root record class
     * @param leafClass the leaf component type
     * @param path      the component path from root to leaf
     */
    public record RecordLensPlan(MethodHandles.Lookup lookup, Class<?> rootClass, Class<?> leafClass, List<String> path) {
        public RecordLensPlan {
            Objects.requireNonNull(lookup);
            path = List.copyOf(path);
        }

        @Nullable
        RecordLensPlan compose(RecordLensPlan child) {
            if (!leafClass.equals(child.rootClass)) {
                return null;
            }
            List<String> merged = new ArrayList<>(path.size() + child.path.size());
            merged.addAll(path);
            merged.addAll(child.path);
            return new RecordLensPlan(lookup, rootClass, child.leafClass, merged);
        }
    }
}
