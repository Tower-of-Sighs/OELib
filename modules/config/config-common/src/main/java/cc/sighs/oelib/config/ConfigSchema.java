package cc.sighs.oelib.config;

import cc.sighs.oelib.config.api.IConfigPermissionChecker;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.optics.ConfigLens;
import cc.sighs.oelib.config.util.ConfigCodecUtil;
import cc.sighs.oelib.config.util.ConfigFieldMetaUtil;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Primary entry point for defining configuration units from record codec schemas.
 *
 * <p>An {@code ConfigSchema} definition produces a {@link Definition} that
 * bundles the registered {@link ConfigUnit} with the root record class,
 * enabling lens-based field access through {@link RecordLensBuilder}.
 *
 * <p>Nested records are declared with the static {@link #record(String, Class, Function, Function)}
 * method, which must be called inside an active {@link ConfigContext}.
 * Reusable nested declarations can be expressed through {@link ConfigMetaCodec}
 * and passed to {@link #record(String, Class, ConfigMetaCodec, Function)}.
 *
 * <p>Usage follows this pattern:
 * <pre>{@code
 * var def = ConfigSchema.defineClient(
 *     id, MyConfig.class,
 *     meta -> meta.fileName("my-config"),
 *     instance -> instance.group(
 *         ConfigField.intRange("port", 1, 65535).forGetter(MyConfig::port),
 *         ConfigSchema.record("db", DbConfig.class,
 *             sub -> sub.group(
 *                 ConfigField.string("url").forGetter(DbConfig::url)
 *             ),
 *             MyConfig::db
 *         )
 *     ).apply(instance, MyConfig::new)
 * );
 * }</pre>
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
     * @throws NullPointerException if {@code configId}, {@code rootClass}, or {@code builder} is {@code null}
     */
    public static <T> Definition<T> defineClient(
            ResourceLocation configId,
            Class<T> rootClass,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        return define(configId, rootClass, metaCustomizer, builder, ConfigSide.CLIENT);
    }

    /**
     * Defines a server-side configuration.
     *
     * @param configId       the configuration id
     * @param rootClass      the root record class
     * @param metaCustomizer optional customizer for {@link ConfigMeta}, or {@code null}
     * @param builder        the record codec builder function
     * @param <T>            the type of the configuration record
     * @return a definition holding the unit and root class
     * @throws NullPointerException if {@code configId}, {@code rootClass}, or {@code builder} is {@code null}
     */
    public static <T> Definition<T> defineServer(
            ResourceLocation configId,
            Class<T> rootClass,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        return define(configId, rootClass, metaCustomizer, builder, ConfigSide.SERVER);
    }

    /**
     * Defines a configuration on the given side.
     *
     * @param configId       the configuration id
     * @param rootClass      the root record class
     * @param metaCustomizer optional customizer for {@link ConfigMeta}, or {@code null}
     * @param builder        the record codec builder function
     * @param side           the logical side
     * @param <T>            the type of the configuration record
     * @return a definition holding the unit and root class
     * @throws NullPointerException if {@code configId}, {@code rootClass}, {@code builder},
     *                              or {@code side} is {@code null}
     */
    public static <T> Definition<T> define(
            ResourceLocation configId,
            Class<T> rootClass,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            ConfigSide side
    ) {
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
        return new Definition<>(unit, rootClass);
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
     * @throws NullPointerException  if any argument is {@code null}
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
     * @throws NullPointerException if any argument is {@code null}
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
     * @throws NullPointerException if any argument is {@code null}
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
     * @throws NullPointerException if {@code recordClass} or {@code builder} is {@code null}
     */
    public static <T> ConfigMetaCodec<T> metaCodec(
            Class<T> recordClass,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        return ConfigMetaCodec.of(recordClass, builder);
    }

    private static <C> void recordNestedMeta(String key, Class<C> recordClass, ConfigMetaCodec<C> metaCodec) {
        if (!ConfigContext.isActive()) {
            return;
        }
        ConfigContext.withRecord(key, recordClass, () -> {
            ResourceLocation configId = ConfigContext.currentConfigId();
            for (ConfigValueMeta localMeta : metaCodec.fields()) {
                ConfigField.recordMeta(rewriteNestedMeta(localMeta, configId));
            }
            return null;
        });
    }

    private static ConfigValueMeta rewriteNestedMeta(ConfigValueMeta meta, ResourceLocation configId) {
        ConfigValueMeta.Builder builder = ConfigValueMeta.builder(meta.key())
                .comment(meta.comment().orElse(null))
                .uiHint(meta.uiHint().orElse(null))
                .hidden(meta.hidden())
                .validators(meta.validators())
                .migrations(meta.migrations());

        if (configId != null) {
            String autoKey = ConfigFieldMetaUtil.autoTranslationKey(configId, meta.key());
            builder.translationKey(autoKey);
            if (meta.tooltip().isPresent()) {
                builder.tooltip(autoKey + ".tooltip");
            }
        } else {
            builder.translationKey(meta.translationKey().orElse(null));
            builder.tooltip(meta.tooltip().orElse(null));
        }

        return ConfigFieldMetaUtil.qualifyForContext(builder.build());
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

        private Definition(ConfigUnit<T> unit, Class<T> rootClass) {
            this.unit = unit;
            this.rootClass = rootClass;
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
         * Creates a {@link ConfigLens} for the given record component accessor
         * using the default lookup.
         *
         * @param getter a serializable method reference to a record component
         * @param <V>    the type of the component value
         * @return a lens targeting that component
         */
        @ApiStatus.Internal
        public <V> ConfigLens<T, V> lens(RecordLensBuilder.LensGetter<T, V> getter) {
            return RecordLensBuilder.lens(rootClass, getter);
        }

        /**
         * Creates a {@link ConfigLens} for the given record component accessor
         * using an explicit lookup.
         *
         * @param lookup the lookup for access control
         * @param getter a serializable method reference to a record component
         * @param <V>    the type of the component value
         * @return a lens targeting that component
         */
        @ApiStatus.Internal
        public <V> ConfigLens<T, V> lens(MethodHandles.Lookup lookup, RecordLensBuilder.LensGetter<T, V> getter) {
            return RecordLensBuilder.lens(lookup, rootClass, getter);
        }

        /**
         * Creates a {@link ConfigLens} for a component identified by name.
         *
         * @param componentName the name of the record component
         * @param <V>           the type of the component value
         * @return a lens targeting that component
         * @throws IllegalArgumentException if the component does not exist
         */
        @ApiStatus.Internal
        public <V> ConfigLens<T, V> lens(String componentName) {
            return RecordLensBuilder.lens(rootClass, componentName);
        }

        /**
         * Creates a {@link ConfigLens} for a component identified by name
         * using an explicit lookup.
         *
         * @param lookup        the lookup for access control
         * @param componentName the name of the record component
         * @param <V>           the type of the component value
         * @return a lens targeting that component
         * @throws IllegalArgumentException if the component does not exist
         */
        public <V> ConfigLens<T, V> lens(MethodHandles.Lookup lookup, String componentName) {
            return RecordLensBuilder.lens(lookup, rootClass, componentName);
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
