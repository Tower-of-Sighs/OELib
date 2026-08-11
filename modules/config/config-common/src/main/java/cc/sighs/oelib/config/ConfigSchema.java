package cc.sighs.oelib.config;

import cc.sighs.oelib.config.codecs.ConfigMetaCodec;
import cc.sighs.oelib.config.codecs.ConfigSealedCodec;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.util.ConfigCodecUtil;
import cc.sighs.oelib.config.util.ConfigFieldMetaUtil;
import com.flechazo.optics.LensGetter;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Defines configuration units from record codec schemas.
 *
 * <p>Each definition associates the supplied lookup with the root record type
 * and returns a {@link ConfigUnit} for that type. Nested records are declared through
 * {@link #record(String, Class, Function, LensGetter)} or through reusable
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
     * @param projectLookup  a full-privilege lookup belonging to the root type's module
     * @param configId       the configuration id
     * @param rootClass      the root record class
     * @param metaCustomizer optional customizer for {@link ConfigMeta}, or {@code null}
     * @param builder        the record codec builder function
     * @param <T>            the type of the configuration record
     * @return the configuration unit bound to {@code rootClass}
     */
    public static <T> ConfigUnit<T> defineClient(
            MethodHandles.Lookup projectLookup,
            ResourceLocation configId,
            Class<T> rootClass,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        return define(projectLookup, configId, rootClass, metaCustomizer, builder, ConfigSide.CLIENT);
    }

    /**
     * Defines a server-side configuration.
     *
     * @param projectLookup  a full-privilege lookup belonging to the root type's module
     * @param configId       the configuration id
     * @param rootClass      the root record class
     * @param metaCustomizer optional customizer for {@link ConfigMeta}, or {@code null}
     * @param builder        the record codec builder function
     * @param <T>            the type of the configuration record
     * @return the configuration unit bound to {@code rootClass}
     */
    public static <T> ConfigUnit<T> defineServer(
            MethodHandles.Lookup projectLookup,
            ResourceLocation configId,
            Class<T> rootClass,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        return define(projectLookup, configId, rootClass, metaCustomizer, builder, ConfigSide.SERVER);
    }

    /**
     * Defines a configuration on the given side.
     *
     * @param projectLookup  a full-privilege lookup belonging to the root type's module
     * @param configId       the configuration id
     * @param rootClass      the root record class
     * @param metaCustomizer optional customizer for {@link ConfigMeta}, or {@code null}
     * @param builder        the record codec builder function
     * @param side           the logical side
     * @param <T>            the type of the configuration record
     * @return the configuration unit bound to {@code rootClass}
     */
    public static <T> ConfigUnit<T> define(
            MethodHandles.Lookup projectLookup,
            ResourceLocation configId,
            Class<T> rootClass,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            ConfigSide side
    ) {
        Objects.requireNonNull(projectLookup);
        Objects.requireNonNull(configId);
        Objects.requireNonNull(rootClass);
        Objects.requireNonNull(builder);
        Objects.requireNonNull(side);

        ConfigOpticsLookupProvider.register(rootClass, projectLookup);

        List<ConfigValueMeta> fields = new ArrayList<>();
        Codec<T> codec = ConfigContext.withRoot(configId, rootClass, fields, () -> RecordCodecBuilder.create(builder));
        ConfigMeta meta = buildMeta(configId, side, metaCustomizer);
        ConfigCodec<T> configCodec = new ConfigCodec<>(codec, meta, List.copyOf(fields));
        T defaultValue = ConfigCodecUtil.deriveDefault(codec);
        return ConfigUnit.of(rootClass, configCodec, defaultValue);
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
     * @param getter the parent record component accessor
     * @param <P>         the parent record type
     * @param <C>         the child record type
     * @return a record codec builder for the nested field
     * @throws IllegalStateException if no {@link ConfigContext} is active
     */
    public static <P, C> RecordCodecBuilder<P, C> record(
            String key,
            Class<C> recordClass,
            Function<RecordCodecBuilder.Instance<C>, ? extends App<RecordCodecBuilder.Mu<C>, C>> builder,
            LensGetter<P, C> getter
    ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(builder);
        Objects.requireNonNull(getter);
        Codec<C> nested = ConfigContext.withRecord(
                key, recordClass, getter, () -> RecordCodecBuilder.create(builder));
        C nestedDefault = ConfigCodecUtil.deriveDefault(nested);
        return nested.fieldOf(key).orElse(nestedDefault).forGetter(getter);
    }

    /**
     * Creates a builder for a nested record field that uses a pre-built codec.
     *
     * @param key          the field key for this nested record within the parent
     * @param codec        the pre-built codec for the child type
     * @param defaultValue the default value for the child type
     * @param getter the parent record component accessor
     * @param <P>          the parent record type
     * @param <C>          the child record type
     * @return a record codec builder for the nested field
     */
    public static <P, C> RecordCodecBuilder<P, C> record(
            String key,
            Codec<C> codec,
            C defaultValue,
            LensGetter<P, C> getter
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
     * @param getter the parent record component accessor
     * @param <P>         the parent record type
     * @param <C>         the child record type
     * @return a record codec builder for the nested field
     */
    public static <P, C> RecordCodecBuilder<P, C> record(
            String key,
            Class<C> recordClass,
            ConfigMetaCodec<C> metaCodec,
            LensGetter<P, C> getter
    ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(metaCodec);
        Objects.requireNonNull(getter);

        recordNestedMeta(key, recordClass, getter, metaCodec);
        return metaCodec.fieldOf(key).orElse(metaCodec.defaultValue()).forGetter(getter);
    }

    /**
     * Creates a reusable metadata codec for a record schema.
     *
     * @param recordClass the record class represented by the codec
     * @param builder     the record codec builder function
     * @param <T>         the record type
     * @return a metadata codec that can be embedded with
     *         {@link #record(String, Class, ConfigMetaCodec, LensGetter)}
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

    private static <P, C> void recordNestedMeta(
            String key, Class<C> recordClass, LensGetter<P, C> getter,
            ConfigMetaCodec<C> metaCodec) {
        if (!ConfigContext.isActive()) {
            return;
        }
        ConfigContext.withRecord(key, recordClass, getter, () -> {
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

}
