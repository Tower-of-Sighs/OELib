package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.api.ConfigEvents;
import cc.sighs.oelib.config.datafix.ConfigFixRegistry;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.optics.ConfigLens;
import cc.sighs.oelib.config.optics.ConfigPrism;
import cc.sighs.oelib.config.util.ConfigIOUtil;
import cc.sighs.oelib.config.util.ConfigMigrationUtil;
import cc.sighs.oelib.config.util.ConfigPathUtil;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import cc.sighs.oelib.platform.Platform;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

/**
 * A runtime unit that owns one configuration instance: its codec, its
 * current value, and its persistence lifecycle.
 *
 * <p>An {@code ConfigUnit} lazily loads its value from disk on the first
 * call to {@link #get()}. Subsequent reads return the cached value.
 * Mutations go through {@link #update(ConfigLens, UnaryOperator)} or
 * {@link #updateAll(ConfigMutation[])} and are validated, persisted,
 * and broadcast as change events in a single atomic step.
 *
 * <p>On load failure the unit falls back through: last valid value,
 * current cached value, and finally the default value provided at
 * construction time.
 *
 * <p>Instances are created via {@link #of(ConfigCodec, Object)} or
 * through the {@link ConfigManager} registration helpers.
 *
 * @param <T> the type of the configuration value
 */
public class ConfigUnit<T> {
    private final ConfigCodec<T> configCodec;
    private final T defaultValue;
    private final AtomicBoolean loaded = new AtomicBoolean();
    private volatile T currentValue;
    private volatile T lastValidValue;

    private ConfigUnit(ConfigCodec<T> configCodec, T defaultValue) {
        this.configCodec = configCodec;
        this.defaultValue = defaultValue;
    }

    /**
     * Creates a new configuration unit.
     *
     * @param codec        the codec and metadata for this configuration
     * @param defaultValue the default value used when no persisted file exists
     * @param <T>          the type of the configuration value
     * @return a new configuration unit
     * @throws NullPointerException if {@code codec} or {@code defaultValue} is {@code null}
     */
    public static <T> ConfigUnit<T> of(ConfigCodec<T> codec, T defaultValue) {
        Objects.requireNonNull(codec);
        Objects.requireNonNull(defaultValue);
        return new ConfigUnit<>(codec, defaultValue);
    }

    /**
     * Returns the current configuration value, loading from disk on first access.
     *
     * <p>On load failure, a fallback value is returned: the last valid value,
     * the current cached value, or the default value, in that order.
     *
     * @return the current configuration value
     */
    public T get() {
        if (loaded.compareAndSet(false, true)) {
            try {
                T value = load();
                currentValue = value;
                lastValidValue = value;
            } catch (Exception e) {
                T fallback = fallbackValue();
                OELib.LOGGER.error("Failed to load config {}, fallback to last valid value", configCodec.meta().id(), e);
                currentValue = fallback;
            }
        }
        return currentValue;
    }

    /**
     * Reloads the configuration value from disk, replacing the cached value.
     */
    public void reload() {
        try {
            T value = load();
            currentValue = value;
            lastValidValue = value;
        } catch (Exception e) {
            T fallback = fallbackValue();
            OELib.LOGGER.error("Failed to reload config {}, keeping last valid value", configCodec.meta().id(), e);
            currentValue = fallback;
        }
    }

    /**
     * Runs automatic migration on registration if the configuration file
     * exists on disk.
     */
    public void applyAutoMigrationOnRegister() {
        ConfigIOUtil.applyAutoMigrationOnRegister(this);
    }

    private T load() {
        var meta = configCodec.meta();
        var path = ConfigIOUtil.resolveLoadPath(meta);
        var loaded = ConfigSerializationUtil.loadFromFile(path, meta.format(), configCodec.codec(), defaultValue);
        T value = loaded.orElse(defaultValue);
        value = ConfigMigrationUtil.applyFieldMigrations(value, configCodec.codec(), configCodec.fields());
        validateValueOrThrow(value);
        OELib.LOGGER.debug("Loaded config {} from {}", meta.id(), path);
        ConfigEvents.onLoad(this, value);
        return value;
    }

    /**
     * Persists the current configuration value to disk.
     *
     * <p>On failure the unit rolls back to the last known valid value.
     */
    public void save() {
        T value = currentValue != null ? currentValue : defaultValue;
        try {
            validateValueOrThrow(value);
            writeToDisk(value);
            lastValidValue = value;
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to save config {}", configCodec.meta().id(), e);
            rollbackToLastValid();
        }
    }

    /**
     * Returns the default value provided at construction time.
     *
     * @return the default configuration value
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the configuration metadata.
     *
     * @return the metadata, never {@code null}
     */
    public ConfigMeta meta() {
        return configCodec.meta();
    }

    /**
     * Returns the configuration identifier.
     *
     * @return the identifier
     */
    public Identifier id() {
        return configCodec.meta().id();
    }

    /**
     * Returns the codec and metadata for this configuration.
     *
     * @return the {@link ConfigCodec}
     */
    public ConfigCodec<T> codec() {
        return configCodec;
    }

    /**
     * Atomically sets the configuration value and fires a change event.
     *
     * <p>When the platform is a client and this unit is server-side and a
     * server update is in progress, the change event is suppressed to avoid
     * double-processing.
     *
     * @param value the new value
     */
    public void setValue(T value) {
        var old = this.currentValue;
        this.currentValue = value;
        if (Platform.isClient() && configCodec.meta().side() == ConfigSide.SERVER && ConfigManager.isUpdatingFromServer()) {
            return;
        }
        OELib.LOGGER.info("Config {} changed", configCodec.meta().id());
        ConfigEvents.onChanged(this, old, value);
    }

    /**
     * Reads a single field through the given lens.
     *
     * @param lens the lens targeting the field
     * @param <V>  the type of the field value
     * @return the field value
     * @throws NullPointerException if {@code lens} is {@code null}
     */
    public <V> V view(ConfigLens<T, V> lens) {
        Objects.requireNonNull(lens);
        return lens.view(get());
    }

    /**
     * Updates a single field through the given lens, validates the result,
     * persists it, and fires a change event.
     *
     * @param lens    the lens targeting the field
     * @param updater a function transforming the current field value
     * @param <V>     the type of the field value
     * @return the committed configuration value
     * @throws NullPointerException if {@code lens} or {@code updater} is {@code null}
     * @throws IllegalStateException if validation fails
     */
    public <V> T update(ConfigLens<T, V> lens, UnaryOperator<V> updater) {
        Objects.requireNonNull(lens);
        Objects.requireNonNull(updater);
        T current = get();
        T updated = lens.update(current, updater);
        return commitCandidate(current, updated, true);
    }

    /**
     * Applies multiple mutations in sequence, validates, persists, and fires
     * a change event if the value changed.
     *
     * <p>Null mutations in the array are silently skipped.
     *
     * @param mutations the mutations to apply
     * @return the committed configuration value
     * @throws NullPointerException if {@code mutations} is {@code null}
     */
    @SafeVarargs
    public final T updateAll(ConfigMutation<T>... mutations) {
        Objects.requireNonNull(mutations);
        T current = get();
        T updated = current;
        for (ConfigMutation<T> mutation : mutations) {
            if (mutation == null) {
                continue;
            }
            updated = mutation.apply(updated);
        }
        return commitCandidate(current, updated, true);
    }

    /**
     * Conditionally updates a value through a {@link ConfigPrism}, which may
     * decline to apply the transformation if the value does not match the prism.
     *
     * @param prism   the prism to match against
     * @param updater a function transforming the matched value
     * @param <V>     the type of the matched value
     * @return the committed (or unchanged) configuration value
     * @throws NullPointerException if {@code prism} or {@code updater} is {@code null}
     */
    public <V> T ifPresent(ConfigPrism<T, V> prism, UnaryOperator<V> updater) {
        Objects.requireNonNull(prism);
        Objects.requireNonNull(updater);
        T current = get();
        T updated = prism.updateIfPresent(current, updater);
        return commitCandidate(current, updated, true);
    }

    /**
     * Alias for {@link #ifPresent(ConfigPrism, UnaryOperator)}.
     *
     * @param prism   the prism to match against
     * @param updater a function transforming the matched value
     * @param <V>     the type of the matched value
     * @return the committed (or unchanged) configuration value
     */
    public <V> T whenSubtype(ConfigPrism<T, V> prism, UnaryOperator<V> updater) {
        return ifPresent(prism, updater);
    }

    /**
     * Validates a candidate value, sets it if different from the current value,
     * and optionally persists it to disk.
     *
     * @param previous  the previous configuration value
     * @param candidate the candidate value to commit
     * @param persist   whether to write to disk
     * @return the candidate value
     * @throws IllegalStateException if validation fails
     */
    T commitCandidate(T previous, T candidate, boolean persist) {
        validateValueOrThrow(candidate);
        if (!Objects.equals(candidate, previous)) {
            setValue(candidate);
        }
        if (persist) {
            writeToDisk(candidate);
        }
        lastValidValue = candidate;
        return candidate;
    }

    private void validateValueOrThrow(T value) {
        for (ConfigValueMeta fieldMeta : configCodec.fields()) {
            if (fieldMeta.validators().isEmpty()) {
                continue;
            }
            Object fieldValue = ConfigPathUtil.getObjectByPath(value, fieldMeta.key());
            for (ConfigValueMeta.ConfigValueValidator validator : fieldMeta.validators()) {
                var result = validator.validate(fieldValue, value);
                if (result.isPresent()) {
                    throw new IllegalStateException("Validation failed at '" + fieldMeta.key() + "': " + result.get());
                }
            }
        }
    }

    private void writeToDisk(T value) {
        var meta = configCodec.meta();
        var path = ConfigIOUtil.resolveSavePath(meta);
        ConfigEvents.beforeSave(this, value);
        int version = ConfigFixRegistry.get(meta.id()).map(ConfigFixRegistry.Chain::currentVersion).orElse(0);
        var content = ConfigSerializationUtil.encodeToStringWithVersion(value, version, meta.format(), configCodec.codec(), configCodec.fields());
        if (content.isEmpty()) {
            throw new IllegalStateException("Failed to save config: " + meta.id());
        }
        try {
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content.get(), StandardCharsets.UTF_8);
            OELib.LOGGER.info("Saved config {} to {}", meta.id(), path);
            ConfigEvents.afterSave(this, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write config file: " + meta.id(), e);
        }
    }

    private T fallbackValue() {
        if (lastValidValue != null) {
            return lastValidValue;
        }
        if (currentValue != null) {
            return currentValue;
        }
        return defaultValue;
    }

    private void rollbackToLastValid() {
        T fallback = fallbackValue();
        if (!Objects.equals(currentValue, fallback)) {
            setValue(fallback);
        } else {
            currentValue = fallback;
        }
    }
}
