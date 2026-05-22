package cc.sighs.oelib.config.optics;

import cc.sighs.oelib.config.ConfigMutation;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * An integer-specialized wrapper around a {@link ConfigLens} that exposes
 * primitive-typed accessors to avoid boxing overhead.
 *
 * <p>Instances are obtained via {@link ConfigLens#asInt()} or created
 * directly with {@link #of(ConfigLens)}.
 *
 * @param <S> the source (record) type
 */
public final class ConfigIntLens<S> {
    private final ConfigLens<S, Integer> lens;

    private ConfigIntLens(ConfigLens<S, Integer> lens) {
        this.lens = lens;
    }

    /**
     * Wraps a lens as an integer-specialized lens.
     *
     * @param lens the lens targeting an {@code int} field
     * @param <S>  the source type
     * @return an integer-specialized lens
     * @throws NullPointerException if {@code lens} is {@code null}
     */
    public static <S> ConfigIntLens<S> of(ConfigLens<S, Integer> lens) {
        Objects.requireNonNull(lens);
        return new ConfigIntLens<>(lens);
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
     * Reads the current integer value.
     *
     * @param source the source record
     * @return the field value
     */
    public int viewAsInt(S source) {
        return lens.view(source);
    }

    /**
     * Produces a new record with this field set to the given value.
     *
     * @param source the source record
     * @param value  the new value
     * @return a new record
     */
    public S set(S source, int value) {
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
    public S update(S source, IntUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return lens.updateInt(source, updater);
    }

    /**
     * Returns a mutation that transforms this field.
     *
     * @param updater a function transforming the current value
     * @return a mutation
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public ConfigMutation<S> map(IntUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return source -> update(source, updater);
    }

    /**
     * Returns a mutation that sets this field to a constant.
     *
     * @param value the value to set
     * @return a mutation
     */
    public ConfigMutation<S> setTo(int value) {
        return source -> set(source, value);
    }

    /**
     * Returns the underlying generic lens.
     *
     * @return the raw lens
     */
    public ConfigLens<S, Integer> raw() {
        return lens;
    }
}
