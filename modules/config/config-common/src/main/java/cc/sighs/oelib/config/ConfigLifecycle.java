package cc.sighs.oelib.config;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Provides lifecycle operations for configuration infrastructure.
 *
 * <p>This type is intended for framework integrations rather than configuration
 * consumers.
 */
@ApiStatus.Internal
public final class ConfigLifecycle {
    private ConfigLifecycle() {
    }

    /**
     * Reloads a configuration from its configured storage location.
     *
     * @param unit the configuration to reload
     */
    public static void reload(ConfigUnit<?> unit) {
        Objects.requireNonNull(unit, "unit").reload();
    }

    /**
     * Persists the current configuration value to its configured storage
     * location.
     *
     * @param unit the configuration to persist
     */
    public static void persist(ConfigUnit<?> unit) {
        Objects.requireNonNull(unit, "unit").save();
    }

    /**
     * Replaces the current configuration value and optionally persists it.
     *
     * <p>The replacement is validated before it becomes current. A successful
     * replacement notifies registered change listeners.
     *
     * @param unit the configuration to update
     * @param value the replacement configuration value
     * @param persist {@code true} to persist the replacement; {@code false} to
     *        update only the in-memory value
     * @param <T> the configuration root type
     * @return the committed configuration value
     * @throws IllegalStateException if validation fails
     */
    public static <T> T replace(ConfigUnit<T> unit, T value, boolean persist) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(value, "value");
        T current = unit.get();
        return unit.commitCandidate(current, value, persist);
    }
}
