package cc.sighs.oelib.config.optics;

import cc.sighs.oelib.config.ConfigMutation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.LongUnaryOperator;

/**
 * A long-specialized wrapper around a {@link ConfigLens} that exposes
 * primitive-typed accessors to avoid boxing overhead.
 *
 * <p>Instances are obtained via {@link ConfigLens#asLong()} or created
 * directly with {@link #of(ConfigLens)}.
 *
 * @param <S> the source (record) type
 */
@ApiStatus.Internal
public final class ConfigLongLens<S> {
    private final ConfigLens<S, Long> lens;

    private ConfigLongLens(ConfigLens<S, Long> lens) {
        this.lens = lens;
    }

    /**
     * Wraps a lens as a long-specialized lens.
     *
     * @param lens the lens targeting a {@code long} field
     * @param <S>  the source type
     * @return a long-specialized lens
     * @throws NullPointerException if {@code lens} is {@code null}
     */
    public static <S> ConfigLongLens<S> of(ConfigLens<S, Long> lens) {
        Objects.requireNonNull(lens);
        return new ConfigLongLens<>(lens);
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
     * Reads the current long value.
     *
     * @param source the source record
     * @return the field value
     */
    public long viewAsLong(S source) {
        return lens.view(source);
    }

    /**
     * Produces a new record with this field set to the given value.
     *
     * @param source the source record
     * @param value  the new value
     * @return a new record
     */
    public S set(S source, long value) {
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
    public S update(S source, LongUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return lens.updateLong(source, updater);
    }

    /**
     * Returns a mutation that transforms this field.
     *
     * @param updater a function transforming the current value
     * @return a mutation
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public ConfigMutation<S> map(LongUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return source -> update(source, updater);
    }

    /**
     * Returns a mutation that sets this field to a constant.
     *
     * @param value the value to set
     * @return a mutation
     */
    public ConfigMutation<S> setTo(long value) {
        return source -> set(source, value);
    }

    /**
     * Returns the underlying generic lens.
     *
     * @return the raw lens
     */
    public ConfigLens<S, Long> raw() {
        return lens;
    }
}
