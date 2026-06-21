package cc.sighs.oelib.config;

import cc.sighs.oelib.config.api.IConfigPermissionChecker;
import cc.sighs.oelib.config.codecs.ConfigMetaCodec;
import cc.sighs.oelib.config.codecs.ConfigSealedCodec;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.util.ConfigCodecUtil;
import cc.sighs.oelib.config.util.ConfigFieldMetaUtil;
import com.flechazo.optics.generated.LensGetter;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Defines configuration units from record codec schemas.
 *
 * <p>Each definition produces a {@link Definition} that binds a
 * {@link ConfigUnit}, the root record class, and the lookup used for
 * record-component access. Nested records are declared through
 * {@link #record(String, Class, Function, Function)} or through reusable
 * {@link ConfigMetaCodec} instances.
 *
 * @see ConfigContext
 * @see ConfigField
 * @see RecordLensBuilder
 */
public final class ConfigSchema {
    private ConfigSchema() {
    }

    /**
     * Defines a client-side configuration.
     *
     * @param configId       the configuration id
     * @param rootClass      the root record class
     * @param metaCustomizer optional customizer for {@link ConfigMeta}, or {@code null}
     * @param builder        the record codec builder function
     * @param <T>            the type of the configuration record
     * @return a definition holding the unit and root class
     */
    public static <T> Definition<T> defineClient(
            MethodHandles.Lookup lensLookup,
            ResourceLocation configId,
            Class<T> rootClass,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        return define(lensLookup, configId, rootClass, metaCustomizer, builder, ConfigSide.CLIENT);
    }

    /**
     * Defines a server-side configuration.
     *
     * @param lensLookup     the lookup for lens-based record component access
     * @param configId       the configuration id
     * @param rootClass      the root record class
     * @param metaCustomizer optional customizer for {@link ConfigMeta}, or {@code null}
     * @param builder        the record codec builder function
     * @param <T>            the type of the configuration record
     * @return a definition holding the unit and root class
     */
    public static <T> Definition<T> defineServer(
            MethodHandles.Lookup lensLookup,
            ResourceLocation configId,
            Class<T> rootClass,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        return define(lensLookup, configId, rootClass, metaCustomizer, builder, ConfigSide.SERVER);
    }

    /**
     * Defines a configuration on the given side.
     *
     * @param lensLookup     the lookup for lens-based record component access
     * @param configId       the configuration id
     * @param rootClass      the root record class
     * @param metaCustomizer optional customizer for {@link ConfigMeta}, or {@code null}
     * @param builder        the record codec builder function
     * @param side           the logical side
     * @param <T>            the type of the configuration record
     * @return a definition holding the unit and root class
     *                              or {@code side} is {@code null}
     */
    public static <T> Definition<T> define(
            MethodHandles.Lookup lensLookup,
            ResourceLocation configId,
            Class<T> rootClass,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            ConfigSide side
    ) {
        Objects.requireNonNull(lensLookup);
        Objects.requireNonNull(configId);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(builder);
        Objects.requireNonNull(side);

        List<ConfigValueMeta> fields = new ArrayList<>();
        Codec<T> codec = ConfigContext.withRoot(configId, rootClass, fields, () -> RecordCodecBuilder.create(builder));
        ConfigMeta meta = buildMeta(configId, side, metaCustomizer);
        ConfigCodec<T> configCodec = new ConfigCodec<>(codec, meta, List.copyOf(fields));
        T defaultValue = ConfigCodecUtil.deriveDefault(codec);
        ConfigUnit<T> unit = ConfigUnit.of(configCodec, defaultValue);
        unit.initLensData(rootClass, lensLookup);
        return new Definition<>(unit, rootClass, lensLookup);
    }

    /**
     * Legacy code path that defines a unit without recording the root class.
     *
     * @param configId       the configuration id
     * @param builder        the record codec builder function
     * @param metaCustomizer optional customizer for {@link ConfigMeta}, or {@code null}
     * @param side           the logical side
     * @param <T>            the type of the configuration record
     * @return the constructed configuration unit
     */
    static <T> ConfigUnit<T> defineLegacy(
            ResourceLocation configId,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            ConfigSide side
    ) {
        Objects.requireNonNull(configId);
        Objects.requireNonNull(builder);
        Objects.requireNonNull(side);

        List<ConfigValueMeta> fields = new ArrayList<>();
        Codec<T> codec = ConfigContext.withRoot(configId, fields, () -> RecordCodecBuilder.create(builder));
        ConfigMeta meta = buildMeta(configId, side, metaCustomizer);
        ConfigCodec<T> configCodec = new ConfigCodec<>(codec, meta, List.copyOf(fields));
        T defaultValue = ConfigCodecUtil.deriveDefault(codec);
        return ConfigUnit.of(configCodec, defaultValue);
    }

    /**
     * Creates a builder for a nested record field.
     *
     * <p>This method must be called inside an active {@link ConfigContext} initiated
     * by one of the {@code define} methods.
     *
     * @param key         the field key for this nested record within the parent
     * @param recordClass the class of the nested record
     * @param builder     the record codec builder function for the nested record
     * @param getter      the accessor function on the parent record
     * @param <P>         the parent record type
     * @param <C>         the child record type
     * @return a record codec builder for the nested field
     * @throws IllegalStateException if no {@link ConfigContext} is active
     */
    public static <P, C> RecordCodecBuilder<P, C> record(
            String key,
            Class<C> recordClass,
            Function<RecordCodecBuilder.Instance<C>, ? extends App<RecordCodecBuilder.Mu<C>, C>> builder,
            Function<P, C> getter
    ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(builder);
        Objects.requireNonNull(getter);
        Codec<C> nested = ConfigContext.withRecord(key, recordClass, () -> RecordCodecBuilder.create(builder));
        C nestedDefault = ConfigCodecUtil.deriveDefault(nested);
        return nested.fieldOf(key).orElse(nestedDefault).forGetter(getter);
    }

    /**
     * Creates a builder for a nested record field that uses a pre-built codec.
     *
     * @param key          the field key for this nested record within the parent
     * @param codec        the pre-built codec for the child type
     * @param defaultValue the default value for the child type
     * @param getter       the accessor function on the parent record
     * @param <P>          the parent record type
     * @param <C>          the child record type
     * @return a record codec builder for the nested field
     */
    public static <P, C> RecordCodecBuilder<P, C> record(
            String key,
            Codec<C> codec,
            C defaultValue,
            Function<P, C> getter
    ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(defaultValue);
        Objects.requireNonNull(getter);
        return codec.fieldOf(key).orElse(defaultValue).forGetter(getter);
    }

    /**
     * Creates a builder for a nested record field backed by a reusable
     * {@link ConfigMetaCodec}.
     *
     * <p>When this method runs inside an active {@link ConfigContext}, it
     * appends the nested field metadata carried by {@code metaCodec} to the
     * current schema under the qualified path prefix of {@code key}.
     *
     * @param key         the field key for this nested record within the parent
     * @param recordClass the class of the nested record
     * @param metaCodec   the reusable nested codec carrying metadata
     * @param getter      the accessor function on the parent record
     * @param <P>         the parent record type
     * @param <C>         the child record type
     * @return a record codec builder for the nested field
     */
    public static <P, C> RecordCodecBuilder<P, C> record(
            String key,
            Class<C> recordClass,
            ConfigMetaCodec<C> metaCodec,
            Function<P, C> getter
    ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(metaCodec);
        Objects.requireNonNull(getter);

        recordNestedMeta(key, recordClass, metaCodec);
        return metaCodec.fieldOf(key).orElse(metaCodec.defaultValue()).forGetter(getter);
    }

    /**
     * Creates a reusable metadata codec for a record schema.
     *
     * @param recordClass the record class represented by the codec
     * @param builder     the record codec builder function
     * @param <T>         the record type
     * @return a metadata codec that can be embedded with {@link #record(String, Class, ConfigMetaCodec, Function)}
     */
    public static <T> ConfigMetaCodec<T> metaCodec(
            Class<T> recordClass,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        return ConfigMetaCodec.of(recordClass, builder);
    }

    /**
     * Creates a reusable metadata codec for a discriminated sealed hierarchy.
     *
     * @param  baseClass the sealed base type
     * @param  typeKey the discriminator field name
     * @param  typeGetter the function that resolves the discriminator id
     * @param  defaultValue the default value
     * @param  variants the declared variants
     * @param  <T> the sealed base type
     * @return a sealed codec carrying variant metadata
     */
    @SafeVarargs
    public static <T> ConfigSealedCodec<T> sealedCodec(
            Class<T> baseClass,
            String typeKey,
            Function<? super T, String> typeGetter,
            T defaultValue,
            ConfigSealedCodec.Variant<? extends T>... variants
    ) {
        return ConfigSealedCodec.of(baseClass, typeKey, typeGetter, defaultValue, variants);
    }

    /**
     * Creates a declared sealed variant.
     *
     * @param  id the discriminator id
     * @param  variantClass the concrete variant class
     * @param  codec the metadata codec for the concrete variant
     * @param  <T> the sealed base type
     * @param  <X> the concrete variant type
     * @return a declared sealed variant
     */
    public static <T, X extends T> ConfigSealedCodec.Variant<X> sealedVariant(String id, Class<X> variantClass, ConfigMetaCodec<X> codec) {
        return ConfigSealedCodec.variant(id, variantClass, codec);
    }

    private static <C> void recordNestedMeta(String key, Class<C> recordClass, ConfigMetaCodec<C> metaCodec) {
        if (!ConfigContext.isActive()) {
            return;
        }
        ConfigContext.withRecord(key, recordClass, () -> {
            ResourceLocation configId = ConfigContext.currentConfigId();
            for (ConfigValueMeta localMeta : metaCodec.fields()) {
                ConfigField.recordMeta(ConfigFieldMetaUtil.rewriteNestedMetaForContext(localMeta, configId));
            }
            return null;
        });
    }

    private static ConfigMeta buildMeta(ResourceLocation configId, ConfigSide side, Consumer<ConfigMeta.Builder> metaCustomizer) {
        var metaBuilder = ConfigMeta.builder(configId)
                .format(ConfigStorageFormat.TOML)
                .side(side);
        if (metaCustomizer != null) {
            metaCustomizer.accept(metaBuilder);
        }
        return metaBuilder.build();
    }

    /**
     * The result of a {@link ConfigSchema} definition, holding both the
     * registered {@link ConfigUnit} and the root record class for lens creation.
     *
     * @param <T> the type of the configuration record
     */
    public static final class Definition<T> {
        private final ConfigUnit<T> unit;
        private final Class<T> rootClass;
        private final MethodHandles.Lookup lensLookup;

        private Definition(ConfigUnit<T> unit, Class<T> rootClass, MethodHandles.Lookup lensLookup) {
            this.unit = unit;
            this.rootClass = rootClass;
            this.lensLookup = lensLookup;
        }

        /**
         * Returns the registered configuration unit.
         *
         * @return the configuration unit
         */
        public ConfigUnit<T> unit() {
            return unit;
        }

        /**
         * Returns the root record class.
         *
         * @return the root record class
         */
        public Class<T> rootClass() {
            return rootClass;
        }

        /**
         * Returns the lookup used for lens-based record component access.
         *
         * @return the lookup
         */
        public MethodHandles.Lookup lensLookup() {
            return lensLookup;
        }

        /**
         * Returns a path selecting exactly one root component.
         *
         * @param  getter the root component accessor
         * @param  <V> the component type
         * @return a path selecting the given component
         */
        public <V> ConfigPath.One<T, V> path(LensGetter<T, V> getter) {
            return ConfigPath.one(lensLookup, rootClass, getter);
        }

        /**
         * Returns a path selecting the present value of an
         * {@link java.util.Optional}-typed root component.
         *
         * @param  getter the optional root component accessor
         * @param  <V> the optional element type
         * @return a path selecting the present optional value
         */
        public <V> ConfigPath.Maybe<T, V> pathOptional(LensGetter<T, Optional<V>> getter) {
            return ConfigPath.optional(lensLookup, rootClass, getter);
        }

        /**
         * Returns a path selecting a root component when it is an instance of
         * the given subtype.
         *
         * @param  getter the root component accessor
         * @param  subtypeClass the required subtype
         * @param  <V> the base type
         * @param  <X> the subtype
         * @return a path selecting the component when it is of the given subtype
         */
        public <V, X extends V> ConfigPath.Maybe<T, X> pathSubtype(LensGetter<T, V> getter, Class<X> subtypeClass) {
            return ConfigPath.subtype(lensLookup, rootClass, getter, subtypeClass);
        }

        /**
         * Returns a path selecting all elements of a list-valued root
         * component.
         *
         * @param  getter the list root component accessor
         * @param  <E> the list element type
         * @return a path selecting all list elements
         */
        public <E> ConfigPath.Many<T, E> pathEach(LensGetter<T, List<E>> getter) {
            return ConfigPath.each(lensLookup, rootClass, getter);
        }

        /**
         * Returns a path selecting all values of a map-valued root component.
         *
         * @param  getter the map root component accessor
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting all map values
         */
        public <K, V> ConfigPath.Many<T, V> pathValues(LensGetter<T, Map<K, V>> getter) {
            return ConfigPath.values(lensLookup, rootClass, getter);
        }

        /**
         * Returns a path selecting all keys of a map-valued root component.
         *
         * @param  getter the map root component accessor
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting all map keys
         */
        public <K, V> ConfigPath.Many<T, K> pathKeys(LensGetter<T, Map<K, V>> getter) {
            return ConfigPath.keys(lensLookup, rootClass, getter);
        }

        /**
         * Returns a path selecting the value stored at the given key in a
         * map-valued root component.
         *
         * @param  getter the map root component accessor
         * @param  key the map key to resolve
         * @param  <K> the map key type
         * @param  <V> the map value type
         * @return a path selecting the map value stored at {@code key}
         */
        public <K, V> ConfigPath.Maybe<T, V> pathValue(LensGetter<T, Map<K, V>> getter, K key) {
            return ConfigPath.value(lensLookup, rootClass, getter, key);
        }

        /**
         * Registers this configuration as a client-side unit.
         */
        public void registerClient() {
            ConfigManager.registerClient(unit);
        }

        /**
         * Registers this configuration as a server-side unit with the given
         * permission checker.
         *
         * @param checker the permission checker for client-originated updates
         */
        public void registerServer(IConfigPermissionChecker checker) {
            ConfigManager.registerServer(unit, checker);
        }
    }
}
