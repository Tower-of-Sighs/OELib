package cc.sighs.oelib.config;

import cc.sighs.oelib.config.api.ConfigEvents;
import cc.sighs.oelib.config.datafix.ConfigFixRegistry;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.util.ConfigIOUtil;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import cc.sighs.oelib.config.validation.ConfigValidationException;
import cc.sighs.oelib.config.validation.ConfigValidationReport;
import cc.sighs.oelib.config.validation.ConfigViolation;
import cc.sighs.oelib.platform.Platform;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.business.core.Traverses;
import com.flechazo.hkt.business.data.NonEmptyList;
import com.flechazo.hkt.business.util.OptionalOps;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.*;
import com.flechazo.optics.focus.AffinePath;
import com.flechazo.optics.focus.FocusPath;
import com.flechazo.optics.focus.TraversalPath;
import com.flechazo.optics.util.Traversals;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Provides access to one configuration value and its persistence lifecycle.
 *
 * <p>The first call to {@link #get()} loads, migrates, decodes, and validates the configured file.
 * Later reads return the accepted in-memory value. A successful persisted mutation validates and
 * writes its candidate before replacing the in-memory value and publishing a change event. A
 * failed validation or write leaves the preceding value unchanged. A mutation equal to the
 * preceding value performs no write and publishes no change event.
 *
 * <p>When loading or reloading fails, the unit uses the most recently validated and accepted value,
 * then the current in-memory value, then the configured default value. The accepted value may have
 * been committed without persistence and therefore is not necessarily the value stored on disk.
 *
 * <p>Instances are created by {@link #of(Class, ConfigCodec, Object)} or a
 * {@link ConfigSchema} definition.
 *
 * @param <T> the type of the configuration value
 */
public class ConfigUnit<T> {
    private final Class<T> rootClass;
    private final ConfigCodec<T> configCodec;
    private final T defaultValue;
    private final AtomicBoolean loaded = new AtomicBoolean();
    private volatile T currentValue;
    private volatile T lastValidValue;

    private ConfigUnit(Class<T> rootClass, ConfigCodec<T> configCodec, T defaultValue) {
        this.rootClass = Objects.requireNonNull(rootClass, "rootClass");
        this.configCodec = configCodec;
        this.defaultValue = defaultValue;
    }

    /**
     * Creates a new configuration unit.
     *
     * @param rootClass the configuration record type
     * @param codec the codec and metadata for this configuration
     * @param defaultValue the default value used when no persisted file exists
     * @param <T> the type of the configuration value
     * @return a new configuration unit
     */
    public static <T> ConfigUnit<T> of(Class<T> rootClass, ConfigCodec<T> codec, T defaultValue) {
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(defaultValue);
        return new ConfigUnit<>(rootClass, codec, defaultValue);
    }

    /**
     * Returns the root record type of this configuration.
     *
     * @return the configuration root type
     */
    public Class<T> rootClass() {
        return rootClass;
    }

    /**
     * Returns the current configuration value, loading from disk on first access.
     *
     * <p>When loading, migration, decoding, or validation fails, this method returns the most
     * recently validated and accepted value, the current in-memory value, or the default value, in
     * that order. The failure is logged and the source file is not modified.
     *
     * @return the current configuration value
     */
    public T get() {
        if (loaded.compareAndSet(false, true)) {
            Try.of(this::load).match(value -> {
                currentValue = value;
                lastValidValue = value;
            }, error -> {
                T fallback = fallbackValue();
                OELibConfig.LOGGER.error(
                        "Failed to load config {}, fallback to last valid value",
                        configCodec.meta().id(), error);
                currentValue = fallback;
            });
        }
        return currentValue;
    }

    /**
     * Reloads the configuration value from its configured storage location.
     *
     * <p>A successful reload replaces the current and most recently accepted values. A failed
     * reload logs the failure and restores the most recently accepted value according to the same
     * ordering as {@link #get()}.
     */
    void reload() {
        Try.of(this::load).match(value -> {
            currentValue = value;
            lastValidValue = value;
        }, error -> {
            T fallback = fallbackValue();
            OELibConfig.LOGGER.error(
                    "Failed to reload config {}, keeping last valid value",
                    configCodec.meta().id(), error);
            currentValue = fallback;
        });
    }

    /**
     * Applies configured migrations when this unit is registered.
     *
     * <p>A successfully migrated and decoded value may be rewritten in the configured format. A
     * migration or decoding failure is logged and leaves the source file unchanged.
     */
    void applyAutoMigrationOnRegister() {
        ConfigIOUtil.applyAutoMigrationOnRegister(this);
    }

    private T load() {
        var meta = configCodec.meta();
        var path = ConfigIOUtil.resolveLoadPath(meta);
        if (!Files.exists(path)) {
            validateValueOrThrow(defaultValue);
            return defaultValue;
        }
        String raw = Try.of(() -> Files.readString(path, StandardCharsets.UTF_8)).fold(
                error -> { throw new IllegalStateException(
                        "Failed to read config " + meta.id(), error); },
                content -> content);
        var dynamic = ConfigSerializationUtil.parseToDynamic(raw, meta.format());
        int inputVersion = dynamic.get("__cfg_version").asInt(0);
        var fixed = ConfigFixRegistry.apply(
                meta.id(), dynamic, inputVersion, configCodec.fields());
        if (fixed.left().isPresent()) {
            var error = fixed.left().orElseThrow();
            throw new IllegalStateException(
                    "Config migration failed [" + error.code() + "]: " + error.message(),
                    error.cause());
        }
        dynamic = fixed.right().orElseThrow();
        var decoded = configCodec.codec().parse(dynamic);
        var decodeError = OptionalOps.toMaybe(decoded.error());
        if (decodeError.isDefined()) {
            throw new IllegalStateException(
                    "Config decode failed for " + meta.id() + ": "
                            + decodeError.get().message());
        }
        T value = OptionalOps.toMaybe(decoded.result()).fold(
                () -> { throw new IllegalStateException(
                        "Config decode returned no value for " + meta.id()); },
                decodedValue -> decodedValue);
        validateValueOrThrow(value);
        OELibConfig.LOGGER.debug("Loaded config {} from {}", meta.id(), path);
        ConfigEvents.onLoad(this, value);
        return value;
    }

    /**
     * Persists the current configuration value.
 *
     * <p>A validation or write failure is logged and restores the most recently validated and
     * accepted value. That value is not necessarily the most recently persisted value.
     */
    void save() {
        T value = currentValue != null ? currentValue : defaultValue;
        Try.of(() -> {
            validateValueOrThrow(value);
            writeToDisk(value);
            lastValidValue = value;
            return value;
        }).peekFailure(error -> {
            OELibConfig.LOGGER.error(
                    "Failed to save config {}", configCodec.meta().id(), error);
            rollbackToLastValid();
        });
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
     * Sets the current configuration value and publishes a change event.
     *
     * <p>When the platform is a client and this unit is server-side and a
     * server update is in progress, the change event is suppressed to avoid
     * double-processing.
     *
     * @param value the new value
     */
    void setValue(T value) {
        var old = this.currentValue;
        this.currentValue = value;
        if (Platform.isClient() && configCodec.meta().side() == ConfigSide.SERVER && ConfigManager.isUpdatingFromServer()) {
            return;
        }
        OELibConfig.LOGGER.info("Config {} changed", configCodec.meta().id());
        ConfigEvents.onChanged(this, old, value);
        if (configCodec.meta().side() == ConfigSide.SERVER
                && Platform.isServer()
                && !ConfigManager.isUpdatingFromServer()) {
            ServerConfigManager.synchronizeAccepted(this, value);
        }
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
        return lens(getter).get(get());
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
        var lens = lens(getter);
        T current = get();
        T updated = lens.modify(modifier, current);
        return commitCandidate(current, updated, true);
    }

    /**
     * Replaces a record component, validates the resulting configuration, and
     * persists it.
     *
     * @param getter the accessor identifying the record component
     * @param value the replacement component value
     * @param <V> the component type
     * @return the committed configuration value
     * @throws IllegalStateException if validation or persistence fails
     */
    public <V> T set(LensGetter<T, V> getter, V value) {
        return set(getter, value, true);
    }

    /**
     * Replaces a record component and validates the resulting configuration
     * without persisting it.
     *
     * @param getter the accessor identifying the record component
     * @param value the replacement component value
     * @param <V> the component type
     * @return the committed in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <V> T setNoSave(LensGetter<T, V> getter, V value) {
        return set(getter, value, false);
    }

    private <V> T set(LensGetter<T, V> getter, V value, boolean persist) {
        Objects.requireNonNull(getter, "getter");
        T current = get();
        return commitCandidate(current, lens(getter).set(value, current), persist);
    }

    /**
     * Creates an empty mutation for this configuration.
     *
     * @return an empty mutation that accepts this configuration root type
     */
    public ConfigMutation<T> mutation() {
        return new ConfigMutation<>(rootClass);
    }

    /**
     * Applies a mutation, validates the resulting configuration, and persists
     * it as one committed change.
     *
     * @param mutation the ordered transformations to apply
     * @return the committed configuration value
     * @throws IllegalArgumentException if the mutation belongs to another root type
     * @throws IllegalStateException if validation or persistence fails
     */
    public T applyMutation(ConfigMutation<T> mutation) {
        return applyMutation(mutation, true);
    }

    /**
     * Applies a mutation and validates the resulting configuration without
     * persisting it.
     *
     * @param mutation the ordered transformations to apply
     * @return the committed in-memory configuration value
     * @throws IllegalArgumentException if the mutation belongs to another root type
     * @throws IllegalStateException if validation fails
     */
    public T applyMutationNoSave(ConfigMutation<T> mutation) {
        return applyMutation(mutation, false);
    }

    private T applyMutation(ConfigMutation<T> mutation, boolean persist) {
        Objects.requireNonNull(mutation, "mutation");
        if (mutation.rootClass() != rootClass) {
            throw new IllegalArgumentException(
                    "Mutation for " + mutation.rootClass().getName()
                            + " cannot be applied to " + rootClass.getName());
        }
        T current = get();
        return commitCandidate(current, mutation.apply(current), persist);
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
        return OptionalOps.fromEither(optional(getter).preview(get()));
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
        return OptionalOps.fromEither(subtype(getter, subtype).preview(get()));
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
        var selector = optional(getter);
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
        var selector = subtype(getter, subtype);
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
        var traversal = listTraversal(getter);
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
        var traversal = listTraversal(getter).filtered(predicate);
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
        return listTraversal(getter).getAll(get());
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
        return listTraversal(getter).filtered(predicate).getAll(get());
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
        return listTraversal(getter).length(get());
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
        return listTraversal(getter).exists(predicate, get());
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
        return OptionalOps.fromMaybe(listTraversal(getter).asFold().find(predicate, get()));
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
        return listTraversal(getter).all(predicate, get());
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
        var traversal = mapValuesTraversal(getter);
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
        var traversal = mapValuesTraversal(getter).filtered(predicate);
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
        return mapValuesTraversal(getter).getAll(get());
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
        return mapValuesTraversal(getter).filtered(predicate).getAll(get());
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
        return mapValuesTraversal(getter).length(get());
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
        return mapValuesTraversal(getter).exists(predicate, get());
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
        return mapValuesTraversal(getter).all(predicate, get());
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
        return OptionalOps.fromMaybe(mapValuesTraversal(getter).asFold().find(predicate, get()));
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
        return mapKeysFold(getter).getAll(get());
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
        return mapKeysFold(getter).length(get());
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
        return mapKeysFold(getter).exists(predicate, get());
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
        return mapKeysFold(getter).all(predicate, get());
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
        return OptionalOps.fromMaybe(mapKeysFold(getter).find(predicate, get()));
    }

    private <A> Lens<T, A> lens(LensGetter<T, A> getter) {
        return RecordLensBuilder.lens(rootClass, getter);
    }

    private <A> Affine<T, A> optional(LensGetter<T, Optional<A>> getter) {
        return RecordLensBuilder.optional(lens(getter));
    }

    private <A, X extends A> Affine<T, X> subtype(
            LensGetter<T, A> getter, Class<X> subtypeClass) {
        return RecordLensBuilder.subtype(lens(getter), subtypeClass);
    }

    private <A> Traversal<T, A> listTraversal(LensGetter<T, List<A>> getter) {
        return lens(getter).andThen(Traversals.forList());
    }

    private <K, V> Traversal<T, V> mapValuesTraversal(LensGetter<T, Map<K, V>> getter) {
        return lens(getter).andThen(Traversals.forMapValues());
    }

    private <K, V> Fold<T, K> mapKeysFold(LensGetter<T, Map<K, V>> getter) {
        return lens(getter).andThen(Fold.mapKeys());
    }

    @SuppressWarnings("unchecked")
    private static <A> Class<A> componentClass(Class<?> sourceClass, LensGetter<?, A> getter) {
        return (Class<A>) RecordLensBuilder.componentType(
                sourceClass, RecordLensBuilder.componentName(getter));
    }

    /**
     * Creates a reusable focus that selects one record component.
     *
     * @param getter the accessor identifying the record component
     * @param <A> the component type
     * @return an exactly-one focus for the component
     */
    public <A> ConfigFocus.One<T, A> focus(LensGetter<T, A> getter) {
        Objects.requireNonNull(getter, "getter");
        return new ConfigFocus.One<>(
                componentClass(rootClass, getter),
                FocusPath.of(lens(getter))
        );
    }

    /**
     * Creates a reusable focus for the present value of an {@link Optional}
     * record component.
     *
     * @param getter the accessor identifying the optional component
     * @param <A> the optional value type
     * @return a zero-or-one focus for the present value
     */
    @SuppressWarnings("unchecked")
    public <A> ConfigFocus.Maybe<T, A> focusOptional(LensGetter<T, Optional<A>> getter) {
        Objects.requireNonNull(getter, "getter");
        return new ConfigFocus.Maybe<>(
                (Class<A>) RecordLensBuilder.optionalElementType(getter),
                AffinePath.of(optional(getter))
        );
    }

    /**
     * Creates a reusable focus for a component value matching a runtime subtype.
     *
     * @param getter the accessor identifying the component
     * @param subtypeClass the subtype selected by the focus
     * @param <A> the component base type
     * @param <X> the selected subtype
     * @return a zero-or-one focus for the matching value
     */
    public <A, X extends A> ConfigFocus.Maybe<T, X> focusSubtype(
            LensGetter<T, A> getter, Class<X> subtypeClass) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(subtypeClass, "subtypeClass");
        return new ConfigFocus.Maybe<>(subtypeClass, AffinePath.of(subtype(getter, subtypeClass)));
    }

    /**
     * Creates a reusable focus over all elements of a {@link List} component.
     *
     * @param getter the accessor identifying the list component
     * @param <A> the list element type
     * @return a multi-focus selecting list elements in encounter order
     */
    @SuppressWarnings("unchecked")
    public <A> ConfigFocus.Many<T, A> focusListElements(LensGetter<T, List<A>> getter) {
        Objects.requireNonNull(getter, "getter");
        return new ConfigFocus.Many<>(
                (Class<A>) RecordLensBuilder.listElementType(getter),
                TraversalPath.of(listTraversal(getter))
        );
    }

    /**
     * Creates a reusable focus over all elements of a {@link Set} component.
     *
     * @param getter the accessor identifying the set component
     * @param <A> the set element type
     * @return a multi-focus selecting set elements in encounter order
     */
    @SuppressWarnings("unchecked")
    public <A> ConfigFocus.Many<T, A> focusSetElements(LensGetter<T, Set<A>> getter) {
        Objects.requireNonNull(getter, "getter");
        Class<A> elementClass = (Class<A>) RecordLensBuilder.setElementType(getter);
        return new ConfigFocus.Many<>(elementClass, TraversalPath.of(
                lens(getter).andThen(Traversals.forSet())));
    }

    /**
     * Creates a reusable focus over all elements of an array component.
     *
     * @param getter the accessor identifying the array component
     * @param <A> the array element type
     * @return a multi-focus selecting array elements in index order
     */
    @SuppressWarnings("unchecked")
    public <A> ConfigFocus.Many<T, A> focusArrayElements(LensGetter<T, A[]> getter) {
        Objects.requireNonNull(getter, "getter");
        Class<A> elementClass = (Class<A>) RecordLensBuilder.arrayElementType(getter);
        return new ConfigFocus.Many<>(elementClass, TraversalPath.of(
                lens(getter).andThen(Traversals.forArray(elementClass))));
    }

    /**
     * Creates a reusable focus over all values of a {@link Map} component.
     *
     * @param getter the accessor identifying the map component
     * @param <K> the map key type
     * @param <V> the map value type
     * @return a multi-focus selecting map values in entry encounter order
     */
    @SuppressWarnings("unchecked")
    public <K, V> ConfigFocus.Many<T, V> focusMapValues(LensGetter<T, Map<K, V>> getter) {
        Objects.requireNonNull(getter, "getter");
        return new ConfigFocus.Many<>(
                (Class<V>) RecordLensBuilder.mapValueType(getter),
                TraversalPath.of(mapValuesTraversal(getter))
        );
    }

    /**
     * Creates a reusable focus over all entries of a {@link Map} component.
     *
     * <p>Each selected entry is represented as a key-value tuple. Replacing an
     * entry may therefore replace both its key and value.
     *
     * @param getter the accessor identifying the map component
     * @param <K> the map key type
     * @param <V> the map value type
     * @return a multi-focus selecting map entries in encounter order
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <K, V> ConfigFocus.Many<T, Tuple2<K, V>> focusMapEntries(
            LensGetter<T, Map<K, V>> getter) {
        Objects.requireNonNull(getter, "getter");
        return new ConfigFocus.Many<>((Class) Tuple2.class, TraversalPath.of(
                lens(getter).andThen(Traversals.forMapEntries())));
    }

    /**
     * Creates a reusable focus for the value associated with a map key.
     *
     * @param getter the accessor identifying the map component
     * @param key the key whose associated value is selected
     * @param <K> the map key type
     * @param <V> the map value type
     * @return a zero-or-one focus that is empty when the key is absent
     */
    @SuppressWarnings("unchecked")
    public <K, V> ConfigFocus.Maybe<T, V> focusMapValue(
            LensGetter<T, Map<K, V>> getter, K key) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(key, "key");
        return new ConfigFocus.Maybe<>(
                (Class<V>) RecordLensBuilder.mapValueType(getter),
                AffinePath.of(lens(getter).andThen(Affine.mapValue(key)))
        );
    }

    /**
     * Returns the value selected by an exactly-one focus.
     *
     * @param focus the focus selecting the value
     * @param <A> the focused value type
     * @return the selected value
     */
    public <A> A view(ConfigFocus.One<T, A> focus) {
        Objects.requireNonNull(focus, "focus");
        return focus.prototype().get(get());
    }

    /**
     * Returns the value selected by a zero-or-one focus, if present.
     *
     * @param focus the focus selecting the optional value
     * @param <A> the focused value type
     * @return the selected value, or an empty optional if the focus is absent
     */
    public <A> Optional<A> preview(ConfigFocus.Maybe<T, A> focus) {
        Objects.requireNonNull(focus, "focus");
        return OptionalOps.fromMaybe(focus.prototype().preview(get()));
    }

    /**
     * Returns every value selected by a multi-focus in encounter order.
     *
     * @param focus the focus selecting the values
     * @param <A> the focused value type
     * @return an unmodifiable list of selected values
     */
    public <A> List<A> getAll(ConfigFocus.Many<T, A> focus) {
        Objects.requireNonNull(focus, "focus");
        return focus.prototype().getAll(get());
    }

    /**
     * Transforms the value selected by an exactly-one focus and persists the
     * resulting configuration.
     *
     * @param focus the focus selecting the value to transform
     * @param updater the focused value transformation
     * @param <A> the focused value type
     * @return the committed configuration value
     * @throws IllegalStateException if validation or persistence fails
     */
    public <A> T update(ConfigFocus.One<T, A> focus, UnaryOperator<A> updater) {
        return updateFocus(focus, updater, true);
    }

    /**
     * Transforms the value selected by an exactly-one focus without persisting
     * the resulting configuration.
     *
     * @param focus the focus selecting the value to transform
     * @param updater the focused value transformation
     * @param <A> the focused value type
     * @return the committed in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <A> T updateNoSave(ConfigFocus.One<T, A> focus, UnaryOperator<A> updater) {
        return updateFocus(focus, updater, false);
    }

    private <A> T updateFocus(
            ConfigFocus.One<T, A> focus, UnaryOperator<A> updater, boolean persist) {
        Objects.requireNonNull(focus, "focus");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(current, focus.prototype().modify(updater, current), persist);
    }

    /**
     * Transforms a present value selected by a zero-or-one focus and persists
     * the resulting configuration.
     *
     * <p>An absent focus leaves the configuration unchanged.
     *
     * @param focus the focus selecting the optional value
     * @param updater the transformation applied to a present value
     * @param <A> the focused value type
     * @return the committed or unchanged configuration value
     * @throws IllegalStateException if validation or persistence fails
     */
    public <A> T ifPresent(ConfigFocus.Maybe<T, A> focus, UnaryOperator<A> updater) {
        return updateOptionalFocus(focus, updater, true);
    }

    /**
     * Transforms a present value selected by a zero-or-one focus without
     * persisting the resulting configuration.
     *
     * <p>An absent focus leaves the configuration unchanged.
     *
     * @param focus the focus selecting the optional value
     * @param updater the transformation applied to a present value
     * @param <A> the focused value type
     * @return the committed or unchanged in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <A> T ifPresentNoSave(ConfigFocus.Maybe<T, A> focus, UnaryOperator<A> updater) {
        return updateOptionalFocus(focus, updater, false);
    }

    private <A> T updateOptionalFocus(
            ConfigFocus.Maybe<T, A> focus, UnaryOperator<A> updater, boolean persist) {
        Objects.requireNonNull(focus, "focus");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(current, focus.prototype().modify(updater, current), persist);
    }

    /**
     * Transforms every value selected by a multi-focus and persists the
     * resulting configuration.
     *
     * @param focus the focus selecting values to transform
     * @param updater the transformation applied to each selected value
     * @param <A> the focused value type
     * @return the committed configuration value
     * @throws IllegalStateException if validation or persistence fails
     */
    public <A> T updateEach(ConfigFocus.Many<T, A> focus, UnaryOperator<A> updater) {
        return updateManyFocus(focus, updater, true);
    }

    /**
     * Transforms every value selected by a multi-focus without persisting the
     * resulting configuration.
     *
     * @param focus the focus selecting values to transform
     * @param updater the transformation applied to each selected value
     * @param <A> the focused value type
     * @return the committed in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <A> T updateEachNoSave(ConfigFocus.Many<T, A> focus, UnaryOperator<A> updater) {
        return updateManyFocus(focus, updater, false);
    }

    private <A> T updateManyFocus(
            ConfigFocus.Many<T, A> focus, UnaryOperator<A> updater, boolean persist) {
        Objects.requireNonNull(focus, "focus");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(current, focus.prototype().modify(updater, current), persist);
    }

    /**
     * Transforms selected values satisfying a predicate and persists the
     * resulting configuration.
     *
     * @param focus the focus selecting candidate values
     * @param predicate the condition selecting values to transform
     * @param updater the transformation applied to matching values
     * @param <A> the focused value type
     * @return the committed configuration value
     * @throws IllegalStateException if validation or persistence fails
     */
    public <A> T updateWhere(
            ConfigFocus.Many<T, A> focus,
            Predicate<? super A> predicate,
            UnaryOperator<A> updater) {
        return updateEach(focus.filter(predicate), updater);
    }

    /**
     * Transforms selected values satisfying a predicate without persisting the
     * resulting configuration.
     *
     * @param focus the focus selecting candidate values
     * @param predicate the condition selecting values to transform
     * @param updater the transformation applied to matching values
     * @param <A> the focused value type
     * @return the committed in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <A> T updateWhereNoSave(
            ConfigFocus.Many<T, A> focus,
            Predicate<? super A> predicate,
            UnaryOperator<A> updater) {
        return updateEachNoSave(focus.filter(predicate), updater);
    }

    /**
     * Transforms a record component without persisting the resulting
     * configuration.
     *
     * @param getter the accessor identifying the record component
     * @param updater the component transformation
     * @param <A> the component type
     * @return the committed in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <A> T updateNoSave(LensGetter<T, A> getter, UnaryOperator<A> updater) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(current, lens(getter).modify(updater, current), false);
    }

    /**
     * Transforms the value of a nonempty {@link Optional} component without
     * persisting the resulting configuration.
     *
     * <p>An empty component remains unchanged.
     *
     * @param getter the accessor identifying the optional component
     * @param updater the transformation applied to a present value
     * @param <A> the optional value type
     * @return the committed or unchanged in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <A> T ifPresentNoSave(
            LensGetter<T, Optional<A>> getter, UnaryOperator<A> updater) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(current, optional(getter).modify(updater, current), false);
    }

    /**
     * Transforms a component value matching a subtype without persisting the
     * resulting configuration.
     *
     * <p>A value of another runtime type remains unchanged.
     *
     * @param getter the accessor identifying the component
     * @param subtypeClass the subtype accepted by the transformation
     * @param updater the transformation applied to a matching value
     * @param <A> the component base type
     * @param <X> the selected subtype
     * @return the committed or unchanged in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <A, X extends A> T whenSubtypeNoSave(
            LensGetter<T, A> getter, Class<X> subtypeClass, UnaryOperator<X> updater) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(subtypeClass, "subtypeClass");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(
                current, subtype(getter, subtypeClass).modify(updater, current), false);
    }

    /**
     * Transforms every element of a {@link List} component without persisting
     * the resulting configuration.
     *
     * @param getter the accessor identifying the list component
     * @param updater the transformation applied to each element
     * @param <A> the list element type
     * @return the committed in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <A> T updateElementsNoSave(
            LensGetter<T, List<A>> getter, UnaryOperator<A> updater) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(current, listTraversal(getter).modify(updater, current), false);
    }

    /**
     * Transforms list elements satisfying a predicate without persisting the
     * resulting configuration.
     *
     * @param getter the accessor identifying the list component
     * @param predicate the condition selecting elements to transform
     * @param updater the transformation applied to matching elements
     * @param <A> the list element type
     * @return the committed in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <A> T updateWhereNoSave(
            LensGetter<T, List<A>> getter,
            Predicate<A> predicate,
            UnaryOperator<A> updater) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(
                current, listTraversal(getter).filtered(predicate).modify(updater, current), false);
    }

    /**
     * Transforms every value of a {@link Map} component without persisting the
     * resulting configuration.
     *
     * @param getter the accessor identifying the map component
     * @param updater the transformation applied to each value
     * @param <K> the map key type
     * @param <V> the map value type
     * @return the committed in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <K, V> T updateValuesNoSave(
            LensGetter<T, Map<K, V>> getter, UnaryOperator<V> updater) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(current, mapValuesTraversal(getter).modify(updater, current), false);
    }

    /**
     * Transforms map values satisfying a predicate without persisting the
     * resulting configuration.
     *
     * @param getter the accessor identifying the map component
     * @param predicate the condition selecting values to transform
     * @param updater the transformation applied to matching values
     * @param <K> the map key type
     * @param <V> the map value type
     * @return the committed in-memory configuration value
     * @throws IllegalStateException if validation fails
     */
    public <K, V> T updateValuesWhereNoSave(
            LensGetter<T, Map<K, V>> getter,
            Predicate<V> predicate,
            UnaryOperator<V> updater) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(updater, "updater");
        T current = get();
        return commitCandidate(
                current, mapValuesTraversal(getter).filtered(predicate).modify(updater, current), false);
    }

    /**
     * Commits a candidate value when it differs from the preceding value.
 *
     * <p>The candidate is validated before any state change. When persistence is requested, the
     * candidate is written before it becomes current. An equal candidate returns {@code previous}
     * without writing or publishing a change event.
     *
     * @param previous the preceding configuration value
     * @param candidate the candidate value to commit
     * @param persist {@code true} to persist the candidate; {@code false} to update memory only
     * @return {@code previous} when both values are equal; otherwise the committed candidate
     * @throws IllegalStateException if validation or requested persistence fails
     */
    synchronized T commitCandidate(T previous, T candidate, boolean persist) {
        validateValueOrThrow(candidate);
        if (Objects.equals(candidate, previous)) {
            return previous;
        }
        if (persist) {
            writeToDisk(candidate);
        }
        setValue(candidate);
        lastValidValue = candidate;
        return candidate;
    }

    private void validateValueOrThrow(T value) {
        validateInternal(value).fold(
                violations -> { throw new ConfigValidationException(
                        new ConfigValidationReport(violations.toList())); },
                accepted -> accepted);
    }

    /**
     * Validates a candidate against every schema rule.
     *
     * <p>All field and cross-field rules are evaluated in schema and registration order. A failed
     * result contains every violation from that evaluation.
     *
     * @param value the complete configuration candidate
     * @return an empty value when validation succeeds, or the complete failure report
     */
    public Optional<ConfigValidationReport> validate(T value) {
        Maybe<ConfigValidationReport> report = validateInternal(value).fold(
                violations -> Maybe.some(new ConfigValidationReport(violations.toList())),
                accepted -> Maybe.none());
        return OptionalOps.fromMaybe(report);
    }

    private Validated<NonEmptyList<ConfigViolation>, T> validateInternal(T value) {
        Objects.requireNonNull(value, "value");
        return Traverses.traverseValidatedNel(
                        configCodec.fields().stream()
                                .filter(field -> !field.validators().isEmpty())
                                .toList(),
                        field -> validateField(field, value))
                .map(ignored -> value);
    }

    private Validated<NonEmptyList<ConfigViolation>, Object> validateField(
            ConfigValueMeta field, T root) {
        Object fieldValue = field.read(root);
        return Traverses.traverseValidatedNel(
                        field.validators(),
                        validator -> validator.validate(fieldValue, root)
                                .mapError(failures -> failures.map(failure -> new ConfigViolation(
                                        field.key(), failure.code(), failure.message(), fieldValue))))
                .map(ignored -> fieldValue);
    }

    private void writeToDisk(T value) {
        var meta = configCodec.meta();
        var path = ConfigIOUtil.resolveSavePath(meta);
        ConfigEvents.beforeSave(this, value);
        int version = ConfigFixRegistry.currentVersion(meta.id(), configCodec.fields());
        var content = ConfigSerializationUtil.encodeToStringWithVersion(value, version, meta.format(), configCodec.codec(), configCodec.fields());
        if (content.isEmpty()) {
            throw new IllegalStateException("Failed to save config: " + meta.id());
        }
        Try.of(() -> {
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(
                    parent != null ? parent : path.toAbsolutePath().getParent(),
                    path.getFileName().toString(), ".tmp");
            try {
                Files.writeString(temp, content.get(), StandardCharsets.UTF_8);
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
            OELibConfig.LOGGER.info("Saved config {} to {}", meta.id(), path);
            ConfigEvents.afterSave(this, value);
            return value;
        }).fold(
                error -> { throw new IllegalStateException(
                        "Failed to write config file: " + meta.id(), error); },
                saved -> saved);
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
