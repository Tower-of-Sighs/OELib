package cc.sighs.oelib.config;

import cc.sighs.oelib.config.api.ConfigEvents;
import cc.sighs.oelib.config.datafix.ConfigFixRegistry;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.util.ConfigIOUtil;
import cc.sighs.oelib.config.util.ConfigMigrationUtil;
import cc.sighs.oelib.config.util.ConfigPathUtil;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import cc.sighs.oelib.platform.Platform;
import com.flechazo.optics.generated.LensGetter;
import com.flechazo.optics.util.Affines;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A runtime unit that owns one configuration instance: its codec, its
 * current value, and its persistence lifecycle.
 *
 * <p>An {@code ConfigUnit} lazily loads its value from disk on the first
 * call to {@link #get()}. Subsequent reads return the cached value.
 * Mutations go through {@link #update(LensGetter, UnaryOperator)} or
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
    private ConfigOpticResolver<T> resolver;

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
                OELibConfig.LOGGER.error("Failed to load config {}, fallback to last valid value", configCodec.meta().id(), e);
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
            OELibConfig.LOGGER.error("Failed to reload config {}, keeping last valid value", configCodec.meta().id(), e);
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
        OELibConfig.LOGGER.debug("Loaded config {} from {}", meta.id(), path);
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
            OELibConfig.LOGGER.error("Failed to save config {}", configCodec.meta().id(), e);
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
     * Returns the configuration id.
     *
     * @return the id
     */
    public ResourceLocation id() {
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
        OELibConfig.LOGGER.info("Config {} changed", configCodec.meta().id());
        ConfigEvents.onChanged(this, old, value);
    }

    /**
     * Returns operations that apply preconstructed paths to this unit.
     *
     * @return path-based operations for this unit
     */
    public ConfigUnitPaths<T> paths() {
        return new ConfigUnitPaths<>(this);
    }

    /**
     * Reads the field identified by the given getter.
     *
     * @param getter a serializable method reference to a record component
     * @param <V>    the field value type
     * @return the field value
     */
    public <V> V view(LensGetter<T, V> getter) {
        Objects.requireNonNull(getter);
        return resolver().lens(getter).get(get());
    }

    /**
     * Updates the field identified by the given getter, validates the result,
     * persists it, and fires a change event.
     *
     * @param getter   a serializable method reference to a record component
     * @param modifier a function transforming the current field value
     * @param <V>      the field value type
     * @return the committed configuration value
     */
    public <V> T update(LensGetter<T, V> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        var lens = resolver().lens(getter);
        T current = get();
        T updated = lens.modify(modifier, current);
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
            if (mutation instanceof ContextualMutation<T> contextual) {
                updated = contextual.apply(updated, resolver());
            } else {
                updated = mutation.apply(updated);
            }
        }
        return commitCandidate(current, updated, true);
    }

    /**
     * Returns the present value of the {@code Optional} field identified by the
     * given getter, if any.
     *
     * @param getter a serializable method reference to an {@code Optional} record component
     * @param <V>    the optional value type
     * @return an {@link Optional} containing the present value, or
     *         {@link Optional#empty()} if the field is empty
     */
    public <V> Optional<V> preview(LensGetter<T, Optional<V>> getter) {
        Objects.requireNonNull(getter);
        return Affines.previewOptional(resolver().optional(getter), get());
    }

    /**
     * Returns the field value when its runtime value is an instance of the
     * specified subtype.
     *
     * @param getter  a serializable method reference to a record component
     * @param subtype the expected subtype class
     * @param <V>     the base field type
     * @param <X>     the subtype to match
     * @return an {@link Optional} containing the matched value, or
     *         {@link Optional#empty()} if the field has a different type
     */
    public <V, X extends V> Optional<X> preview(LensGetter<T, V> getter, Class<X> subtype) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(subtype);
        return Affines.previewOptional(resolver().subtype(getter, subtype), get());
    }

    /**
     * Conditionally updates an {@code Optional} field. If the field is
     * {@link Optional#empty()}, the value is left unchanged.
     *
     * @param getter   a serializable method reference to an {@code Optional} record component
     * @param modifier a function transforming the present value
     * @param <V>      the type of the optional value
     * @return the committed (or unchanged) configuration value
     */
    public <V> T ifPresent(LensGetter<T, Optional<V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        var selector = resolver().optional(getter);
        T current = get();
        T updated = selector.modify(modifier, current);
        return commitCandidate(current, updated, true);
    }

    /**
     * Conditionally updates a field when its runtime value is an instance of
     * the specified subtype. If the value is not of that type, it is left
     * unchanged.
     *
     * @param getter   a serializable method reference to a record component
     * @param subtype  the expected subtype class
     * @param modifier a function transforming the matched value
     * @param <V>      the base field type
     * @param <X>      the subtype to match
     * @return the committed (or unchanged) configuration value
     */
    public <V, X extends V> T whenSubtype(LensGetter<T, V> getter, Class<X> subtype, UnaryOperator<X> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(subtype);
        Objects.requireNonNull(modifier);
        var selector = resolver().subtype(getter, subtype);
        T current = get();
        T updated = selector.modify(modifier, current);
        return commitCandidate(current, updated, true);
    }

    /**
     * Transforms every element of the {@code List} field identified by the
     * given getter and commits the result.
     *
     * @param getter   a serializable method reference to the list field accessor
     * @param modifier a function that transforms each list element
     * @param <V>      the list element type
     * @return the committed configuration value
     */
    public <V> T updateElements(LensGetter<T, List<V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        var traversal = resolver().listTraversal(getter);
        T current = get();
        T updated = traversal.modify(modifier, current);
        return commitCandidate(current, updated, true);
    }

    /**
     * Transforms the elements of the {@code List} field that satisfy the
     * given predicate and commits the result. Elements that do not satisfy
     * the predicate are left unchanged.
     *
     * @param getter    a serializable method reference to the list field accessor
     * @param predicate a predicate that selects which elements to transform
     * @param modifier  a function that transforms each selected element
     * @param <V>       the list element type
     * @return the committed configuration value
     */
    public <V> T updateWhere(LensGetter<T, List<V>> getter, Predicate<V> predicate, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(modifier);
        var traversal = resolver().listTraversal(getter).filtered(predicate);
        T current = get();
        T updated = traversal.modify(modifier, current);
        return commitCandidate(current, updated, true);
    }

    /**
     * Collects all elements of the {@code List} field identified by the
     * given getter.
     *
     * @param getter a serializable method reference to the list field accessor
     * @param <V>    the list element type
     * @return an immutable list of all elements
     */
    public <V> List<V> getAll(LensGetter<T, List<V>> getter) {
        Objects.requireNonNull(getter);
        return resolver().listTraversal(getter).getAll(get());
    }

    /**
     * Collects the elements of the {@code List} field that satisfy the given
     * predicate.
     *
     * @param getter    a serializable method reference to the list field accessor
     * @param predicate a predicate that selects which elements to collect
     * @param <V>       the list element type
     * @return an immutable list of matching elements
     */
    public <V> List<V> getAllWhere(LensGetter<T, List<V>> getter, Predicate<V> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().listTraversal(getter).filtered(predicate).getAll(get());
    }

    /**
     * Returns the number of elements in the {@code List} field identified by
     * the given getter.
     *
     * @param getter a serializable method reference to the list field accessor
     * @param <V>    the list element type
     * @return the number of elements
     */
    public <V> long count(LensGetter<T, List<V>> getter) {
        Objects.requireNonNull(getter);
        return resolver().listTraversal(getter).length(get());
    }

    /**
     * Returns {@code true} if any element of the {@code List} field satisfies
     * the given predicate.
     *
     * @param getter    a serializable method reference to the list field accessor
     * @param predicate a predicate to test against list elements
     * @param <V>       the list element type
     * @return {@code true} if any element satisfies the predicate
     */
    public <V> boolean anyMatch(LensGetter<T, List<V>> getter, Predicate<V> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().listTraversal(getter).exists(predicate, get());
    }

    /**
     * Returns the first element of the {@code List} field that satisfies the
     * given predicate.
     *
     * @param getter    a serializable method reference to the list field accessor
     * @param predicate a predicate to test against list elements
     * @param <V>       the list element type
     * @return an {@link Optional} containing the first matching element, or
     *         {@link Optional#empty()} if no element matches
     */
    public <V> Optional<V> findFirst(LensGetter<T, List<V>> getter, Predicate<V> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().listTraversal(getter).asFold().findOptional(predicate, get());
    }

    /**
     * Returns {@code true} if every element of the {@code List} field
     * satisfies the given predicate, or if the list is empty.
     *
     * @param getter    a serializable method reference to the list field accessor
     * @param predicate a predicate to test against list elements
     * @param <V>       the list element type
     * @return {@code true} if every element satisfies the predicate, or
     *         {@code true} if the list is empty
     */
    public <V> boolean allMatch(LensGetter<T, List<V>> getter, Predicate<V> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().listTraversal(getter).all(predicate, get());
    }

    /**
     * Transforms every value of the {@code Map} field identified by the
     * given getter and commits the result.
     *
     * @param getter   a serializable method reference to the map field accessor
     * @param modifier a function that transforms each map value
     * @param <K>      the map key type
     * @param <V>      the map value type
     * @return the committed configuration value
     */
    public <K, V> T updateValues(LensGetter<T, Map<K, V>> getter, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(modifier);
        var traversal = resolver().mapValuesTraversal(getter);
        T current = get();
        T updated = traversal.modify(modifier, current);
        return commitCandidate(current, updated, true);
    }

    /**
     * Transforms the values of the {@code Map} field that satisfy the given
     * predicate and commits the result. Values that do not satisfy the
     * predicate are left unchanged.
     *
     * @param getter    a serializable method reference to the map field accessor
     * @param predicate a predicate that selects which values to transform
     * @param modifier  a function that transforms each selected value
     * @param <K>       the map key type
     * @param <V>       the map value type
     * @return the committed configuration value
     */
    public <K, V> T updateValuesWhere(LensGetter<T, Map<K, V>> getter, Predicate<V> predicate, UnaryOperator<V> modifier) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(modifier);
        var traversal = resolver().mapValuesTraversal(getter).filtered(predicate);
        T current = get();
        T updated = traversal.modify(modifier, current);
        return commitCandidate(current, updated, true);
    }

    /**
     * Collects all values of the {@code Map} field identified by the given
     * getter.
     *
     * @param getter a serializable method reference to the map field accessor
     * @param <K>    the map key type
     * @param <V>    the map value type
     * @return an immutable list of all values
     */
    public <K, V> List<V> getValues(LensGetter<T, Map<K, V>> getter) {
        Objects.requireNonNull(getter);
        return resolver().mapValuesTraversal(getter).getAll(get());
    }

    /**
     * Collects the values of the {@code Map} field that satisfy the given
     * predicate.
     *
     * @param getter    a serializable method reference to the map field accessor
     * @param predicate a predicate that selects which values to collect
     * @param <K>       the map key type
     * @param <V>       the map value type
     * @return an immutable list of matching values
     */
    public <K, V> List<V> getValuesWhere(LensGetter<T, Map<K, V>> getter, Predicate<V> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().mapValuesTraversal(getter).filtered(predicate).getAll(get());
    }

    /**
     * Returns the number of values in the {@code Map} field identified by the
     * given getter.
     *
     * @param getter a serializable method reference to the map field accessor
     * @param <K>    the map key type
     * @param <V>    the map value type
     * @return the number of values
     */
    public <K, V> long countValues(LensGetter<T, Map<K, V>> getter) {
        Objects.requireNonNull(getter);
        return resolver().mapValuesTraversal(getter).length(get());
    }

    /**
     * Returns {@code true} if any value of the {@code Map} field satisfies the
     * given predicate.
     *
     * @param getter    a serializable method reference to the map field accessor
     * @param predicate a predicate to test against map values
     * @param <K>       the map key type
     * @param <V>       the map value type
     * @return {@code true} if any value satisfies the predicate
     */
    public <K, V> boolean anyValueMatch(LensGetter<T, Map<K, V>> getter, Predicate<V> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().mapValuesTraversal(getter).exists(predicate, get());
    }

    /**
     * Returns {@code true} if every value of the {@code Map} field satisfies
     * the given predicate, or if the map is empty.
     *
     * @param getter    a serializable method reference to the map field accessor
     * @param predicate a predicate to test against map values
     * @param <K>       the map key type
     * @param <V>       the map value type
     * @return {@code true} if every value satisfies the predicate, or
     *         {@code true} if the map is empty
     */
    public <K, V> boolean allValueMatch(LensGetter<T, Map<K, V>> getter, Predicate<V> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().mapValuesTraversal(getter).all(predicate, get());
    }

    /**
     * Returns the first value of the {@code Map} field that satisfies the given
     * predicate.
     *
     * @param getter    a serializable method reference to the map field accessor
     * @param predicate a predicate to test against map values
     * @param <K>       the map key type
     * @param <V>       the map value type
     * @return an {@link Optional} containing the first matching value, or
     *         {@link Optional#empty()} if no value matches
     */
    public <K, V> Optional<V> findValue(LensGetter<T, Map<K, V>> getter, Predicate<V> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().mapValuesTraversal(getter).asFold().findOptional(predicate, get());
    }

    /**
     * Collects all keys of the {@code Map} field identified by the given
     * getter.
     *
     * @param getter a serializable method reference to the map field accessor
     * @param <K>    the map key type
     * @param <V>    the map value type
     * @return an immutable list of all keys
     */
    public <K, V> List<K> getKeys(LensGetter<T, Map<K, V>> getter) {
        Objects.requireNonNull(getter);
        return resolver().mapKeysFold(getter).getAll(get());
    }

    /**
     * Returns the number of keys in the {@code Map} field identified by the
     * given getter.
     *
     * @param getter a serializable method reference to the map field accessor
     * @param <K>    the map key type
     * @param <V>    the map value type
     * @return the number of keys
     */
    public <K, V> long countKeys(LensGetter<T, Map<K, V>> getter) {
        Objects.requireNonNull(getter);
        return resolver().mapKeysFold(getter).length(get());
    }

    /**
     * Returns {@code true} if any key of the {@code Map} field satisfies the
     * given predicate.
     *
     * @param getter    a serializable method reference to the map field accessor
     * @param predicate a predicate to test against map keys
     * @param <K>       the map key type
     * @param <V>       the map value type
     * @return {@code true} if any key satisfies the predicate
     */
    public <K, V> boolean anyKeyMatch(LensGetter<T, Map<K, V>> getter, Predicate<K> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().mapKeysFold(getter).exists(predicate, get());
    }

    /**
     * Returns {@code true} if every key of the {@code Map} field satisfies the
     * given predicate, or if the map is empty.
     *
     * @param getter    a serializable method reference to the map field accessor
     * @param predicate a predicate to test against map keys
     * @param <K>       the map key type
     * @param <V>       the map value type
     * @return {@code true} if every key satisfies the predicate, or
     *         {@code true} if the map is empty
     */
    public <K, V> boolean allKeyMatch(LensGetter<T, Map<K, V>> getter, Predicate<K> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().mapKeysFold(getter).all(predicate, get());
    }

    /**
     * Returns the first key of the {@code Map} field that satisfies the given
     * predicate.
     *
     * @param getter    a serializable method reference to the map field accessor
     * @param predicate a predicate to test against map keys
     * @param <K>       the map key type
     * @param <V>       the map value type
     * @return an {@link Optional} containing the first matching key, or
     *         {@link Optional#empty()} if no key matches
     */
    public <K, V> Optional<K> findKey(LensGetter<T, Map<K, V>> getter, Predicate<K> predicate) {
        Objects.requireNonNull(getter);
        Objects.requireNonNull(predicate);
        return resolver().mapKeysFold(getter).findOptional(predicate, get());
    }

    ConfigOpticResolver<T> resolver() {
        if (resolver == null) {
            throw new IllegalStateException(
                    "This ConfigUnit was constructed without lens initialization. " +
                            "Use ConfigSchema.define() with a MethodHandles.Lookup instead of " +
                            "defineLegacy() or ConfigManager.register(ConfigCodec, T) to enable " +
                            "getter-based API methods."
            );
        }
        return resolver;
    }

    void initLensData(Class<T> rootClass, MethodHandles.Lookup lensLookup) {
        this.resolver = new ConfigOpticResolver<>(lensLookup, rootClass);
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
            OELibConfig.LOGGER.info("Saved config {} to {}", meta.id(), path);
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
