package cc.sighs.oelib.config.optics;

import cc.sighs.oelib.config.ConfigMutation;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/**
 * A double-specialized wrapper around a {@link ConfigLens} that exposes
 * primitive-typed accessors to avoid boxing overhead.
 *
 * <p>Instances are obtained via {@link ConfigLens#asDouble()} or created
 * directly with {@link #of(ConfigLens)}.
 *
 * @param <S> the source (record) type
 */
public final class ConfigDoubleLens<S> {
    private final ConfigLens<S, Double> lens;

    private ConfigDoubleLens(ConfigLens<S, Double> lens) {
        this.lens = lens;
    }

    /**
     * Wraps a lens as a double-specialized lens.
     *
     * @param lens the lens targeting a {@code double} field
     * @param <S>  the source type
     * @return a double-specialized lens
     * @throws NullPointerException if {@code lens} is {@code null}
     */
    public static <S> ConfigDoubleLens<S> of(ConfigLens<S, Double> lens) {
        Objects.requireNonNull(lens);
        return new ConfigDoubleLens<>(lens);
    }

    /**
     * Returns the dotted path to this field.
     *
     * @return the path
     */
    public String path() {
        return lens.path();
    }

    /**
     * Reads the current double value.
     *
     * @param source the source record
     * @return the field value
     */
    public double viewAsDouble(S source) {
        return lens.view(source);
    }

    /**
     * Produces a new record with this field set to the given value.
     *
     * @param source the source record
     * @param value  the new value
     * @return a new record
     */
    public S set(S source, double value) {
        return lens.set(source, value);
    }

    /**
     * Produces a new record with this field transformed by the given operator.
     *
     * @param source  the source record
     * @param updater a function transforming the current value
     * @return a new record
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public S update(S source, DoubleUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return lens.updateDouble(source, updater);
    }

    /**
     * Returns a mutation that transforms this field.
     *
     * @param updater a function transforming the current value
     * @return a mutation
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public ConfigMutation<S> map(DoubleUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return source -> update(source, updater);
    }

    /**
     * Returns a mutation that sets this field to a constant.
     *
     * @param value the value to set
     * @return a mutation
     */
    public ConfigMutation<S> setTo(double value) {
        return source -> set(source, value);
    }

    /**
     * Returns the underlying generic lens.
     *
     * @return the raw lens
     */
    public ConfigLens<S, Double> raw() {
        return lens;
    }
}
