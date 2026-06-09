package cc.sighs.oelib.config.optics;

import cc.sighs.oelib.config.ConfigMutation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * A boolean-specialized wrapper around a {@link ConfigLens} that exposes
 * primitive-typed accessors to avoid boxing overhead.
 *
 * <p>Instances are obtained via {@link ConfigLens#asBoolean()} or created
 * directly with {@link #of(ConfigLens)}.
 *
 * @param <S> the source (record) type
 */
@ApiStatus.Internal
public final class ConfigBooleanLens<S> {
    private final ConfigLens<S, Boolean> lens;

    private ConfigBooleanLens(ConfigLens<S, Boolean> lens) {
        this.lens = lens;
    }

    /**
     * Wraps a lens as a boolean-specialized lens.
     *
     * @param lens the lens targeting a {@code boolean} field
     * @param <S>  the source type
     * @return a boolean-specialized lens
     * @throws NullPointerException if {@code lens} is {@code null}
     */
    public static <S> ConfigBooleanLens<S> of(ConfigLens<S, Boolean> lens) {
        Objects.requireNonNull(lens);
        return new ConfigBooleanLens<>(lens);
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
     * Reads the current boolean value.
     *
     * @param source the source record
     * @return the field value
     */
    public boolean viewAsBoolean(S source) {
        return lens.view(source);
    }

    /**
     * Produces a new record with this field set to the given value.
     *
     * @param source the source record
     * @param value  the new value
     * @return a new record
     */
    public S set(S source, boolean value) {
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
    public S update(S source, ConfigLens.BooleanUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return lens.updateBoolean(source, updater);
    }

    /**
     * Returns a mutation that transforms this field.
     *
     * @param updater a function transforming the current value
     * @return a mutation
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public ConfigMutation<S> map(ConfigLens.BooleanUnaryOperator updater) {
        Objects.requireNonNull(updater);
        return source -> update(source, updater);
    }

    /**
     * Returns a mutation that sets this field to a constant.
     *
     * @param value the value to set
     * @return a mutation
     */
    public ConfigMutation<S> setTo(boolean value) {
        return source -> set(source, value);
    }

    /**
     * Returns the underlying generic lens.
     *
     * @return the raw lens
     */
    public ConfigLens<S, Boolean> raw() {
        return lens;
    }
}
