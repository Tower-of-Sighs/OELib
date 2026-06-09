package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigLens;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Convenience wrapper for reading and writing individual fields of a {@link ConfigUnit}
 * through lenses derived from record accessor method references.
 *
 * <p>An {@code ConfigAccess} instance is bound to a single {@code ConfigUnit}.
 * The {@link #set(Accessor, Object) set} and {@link #lens(Accessor) lens} methods
 * accept a serializable method reference (typically {@code RecordType::component})
 * and resolve it to a {@link ConfigLens} automatically.
 *
 * <p>Null values are rejected at the boundary: every {@code set} method
 * throws {@link NullPointerException} if the value argument is {@code null}.
 *
 * @param <T> the type of the configuration record
 */
@Deprecated
public class ConfigAccess<T> {
    private final ConfigUnit<T> unit;

    /**
     * Constructs an accessor for the given configuration unit.
     *
     * @param unit the configuration unit to read from and write to
     */
    public ConfigAccess(ConfigUnit<T> unit) {
        this.unit = unit;
    }

    /**
     * Sets the field addressed by the given accessor method reference to the
     * specified value, using the caller's lookup context.
     *
     * <p>The accessor must be a serializable method reference to a record
     * component accessor (for example {@code MyConfig::port}). The value is
     * written through a lens and persisted immediately.
     *
     * @param <V>    the type of the field value
     * @param getter a serializable method reference to a record component accessor
     * @param value  the new value; must not be {@code null}
     * @throws NullPointerException if {@code getter} or {@code value} is {@code null}
     */
    public <V> void set(@NotNull Accessor<T, V> getter, @NotNull V value) {
        Objects.requireNonNull(value, "Config value cannot be null");
        unit.update(unit.resolver().lens(getter), ignored -> value);
    }

    /**
     * Sets the field addressed by the given accessor method reference using an
     * explicit lookup for access control.
     *
     * @param <V>    the type of the field value
     * @param lookup the lookup to use for resolving the lens
     * @param getter a serializable method reference to a record component accessor
     * @param value  the new value; must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public <V> void set(MethodHandles.Lookup lookup, @NotNull Accessor<T, V> getter, @NotNull V value) {
        Objects.requireNonNull(value, "Config value cannot be null");
        Objects.requireNonNull(lookup);
        @SuppressWarnings("unchecked")
        Class<T> recordClass = (Class<T>) unit.get().getClass();
        var lens = new ConfigOpticResolver<>(lookup, recordClass).lens(getter);
        unit.update(lens, ignored -> value);
    }

    /**
     * Sets a field using a pre-resolved lens.
     *
     * @param <V>   the type of the field value
     * @param lens  the lens targeting the field
     * @param value the new value; must not be {@code null}
     * @throws NullPointerException if {@code lens} or {@code value} is {@code null}
     */
    public <V> void set(@NotNull ConfigLens<T, V> lens, @NotNull V value) {
        Objects.requireNonNull(lens);
        Objects.requireNonNull(value, "Config value cannot be null");
        unit.update(lens, ignored -> value);
    }

    /**
     * Resolves a {@link ConfigLens} for the given record component accessor
     * using the caller's lookup context.
     *
     * @param <V>    the type of the component value
     * @param getter a serializable method reference to a record component accessor
     * @return a lens targeting that component
     * @throws NullPointerException if {@code getter} is {@code null}
     */
    public <V> ConfigLens<T, V> lens(@NotNull Accessor<T, V> getter) {
        return unit.resolver().lens(getter);
    }

    /**
     * Resolves a {@link ConfigLens} for the given record component accessor
     * using an explicit lookup.
     *
     * @param <V>    the type of the component value
     * @param lookup the lookup to use for resolving the lens
     * @param getter a serializable method reference to a record component accessor
     * @return a lens targeting that component
     * @throws NullPointerException if {@code lookup} or {@code getter} is {@code null}
     */
    public <V> ConfigLens<T, V> lens(MethodHandles.Lookup lookup, @NotNull Accessor<T, V> getter) {
        Objects.requireNonNull(lookup);
        @SuppressWarnings("unchecked")
        Class<T> recordClass = (Class<T>) unit.get().getClass();
        return new ConfigOpticResolver<>(lookup, recordClass).lens(getter);
    }

    /**
     * A serializable function that also implements {@link RecordLensBuilder.LensGetter},
     * allowing method references to record accessors to be passed to {@link ConfigAccess}.
     *
     * @param <T> the record type
     * @param <R> the component type
     */
    @FunctionalInterface
    public interface Accessor<T, R> extends RecordLensBuilder.LensGetter<T, R> {
    }
}