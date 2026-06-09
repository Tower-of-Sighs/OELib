package cc.sighs.oelib.config.codecs;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.ui.ConfigUiHint;
import cc.sighs.oelib.config.util.ConfigPathUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * Codec wrapper for discriminated sealed hierarchies that also carries
 * metadata for the discriminator and variant fields.
 *
 * @param <T> the sealed base type
 */
public final class ConfigSealedCodec<T> implements Codec<T> {
    private final Class<T> baseClass;
    private final String typeKey;
    private final Codec<T> delegate;
    private final T defaultValue;
    private final List<ConfigValueMeta> fields;

    private ConfigSealedCodec(
            Class<T> baseClass,
            String typeKey,
            Codec<T> delegate,
            T defaultValue,
            List<ConfigValueMeta> fields
    ) {
        this.baseClass = baseClass;
        this.typeKey = typeKey;
        this.delegate = delegate;
        this.defaultValue = defaultValue;
        this.fields = List.copyOf(fields);
    }

    /**
     * Creates a sealed codec from discriminated variants.
     *
     * @param  baseClass the sealed base type
     * @param  typeKey the discriminator field name
     * @param  typeGetter the function that resolves the discriminator id from a value
     * @param  defaultValue the default value for the sealed field
     * @param  variants the declared variants
     * @param  <T> the sealed base type
     * @return a sealed codec carrying variant metadata
     */
    @SafeVarargs
    public static <T> ConfigSealedCodec<T> of(
            Class<T> baseClass,
            String typeKey,
            Function<? super T, String> typeGetter,
            T defaultValue,
            Variant<? extends T>... variants
    ) {
        Objects.requireNonNull(baseClass);
        Objects.requireNonNull(typeKey);
        Objects.requireNonNull(typeGetter);
        Objects.requireNonNull(defaultValue);
        Objects.requireNonNull(variants);

        Map<String, Variant<? extends T>> byId = new LinkedHashMap<>();
        for (Variant<? extends T> variant : variants) {
            Objects.requireNonNull(variant);
            Variant<? extends T> previous = byId.putIfAbsent(variant.id(), variant);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate sealed variant id: " + variant.id());
            }
        }
        if (byId.isEmpty()) {
            throw new IllegalArgumentException("At least one sealed variant is required");
        }

        String defaultId = Objects.requireNonNull(typeGetter.apply(defaultValue), "default variant id");
        if (!byId.containsKey(defaultId)) {
            throw new IllegalArgumentException("Default value resolves to unknown sealed variant id: " + defaultId);
        }

        Codec<T> delegate = Codec.STRING.partialDispatch(
                typeKey,
                value -> {
                    String id = Objects.requireNonNull(typeGetter.apply(value), "sealed variant id");
                    if (!byId.containsKey(id)) {
                        return DataResult.error(() -> "Unknown sealed variant id during encode: " + id);
                    }
                    return DataResult.success(id);
                },
                id -> {
                    Variant<? extends T> variant = byId.get(id);
                    if (variant == null) {
                        return DataResult.error(() -> "Unknown sealed variant id: " + id);
                    }
                    return DataResult.success(variant.mapCodec());
                }
        );

        List<String> variantIds = List.copyOf(byId.keySet());
        List<ConfigValueMeta> fields = new ArrayList<>();
        fields.add(
                ConfigValueMeta.builder(typeKey)
                        .uiHint(ConfigUiHint.dropdown(variantIds))
                        .build()
        );

        Map<String, ConfigValueMeta> merged = new LinkedHashMap<>();
        for (Variant<? extends T> variant : byId.values()) {
            for (ConfigValueMeta meta : variant.codec().fields()) {
                ConfigValueMeta conditionedMeta = conditionVariantMeta(typeKey, variant, meta);
                ConfigValueMeta previous = merged.putIfAbsent(conditionedMeta.key(), conditionedMeta);
                if (previous != null) {
                    throw new IllegalArgumentException(
                            "Sealed variants contain duplicate metadata key '" + conditionedMeta.key() +
                                    "'. Flattened sealed fields require distinct field keys across variants."
                    );
                }
            }
        }
        fields.addAll(merged.values());

        return new ConfigSealedCodec<>(baseClass, typeKey, delegate, defaultValue, fields);
    }

    /**
     * Creates a declared sealed variant.
     *
     * @param  id the discriminator id
     * @param  variantClass the concrete variant class
     * @param  codec the metadata codec for the variant
     * @param  <T> the concrete variant type
     * @return a declared sealed variant
     */
    public static <T> Variant<T> variant(String id, Class<T> variantClass, ConfigMetaCodec<T> codec) {
        return new Variant<>(id, variantClass, codec);
    }

    /**
     * Returns the sealed base type.
     *
     * @return the sealed base type
     */
    public Class<T> baseClass() {
        return baseClass;
    }

    /**
     * Returns the discriminator field name.
     *
     * @return the discriminator field name
     */
    public String typeKey() {
        return typeKey;
    }

    /**
     * Returns the default value for this sealed codec.
     *
     * @return the default value
     */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Returns the metadata declared for the discriminator and variant fields.
     *
     * @return an unmodifiable list of metadata entries
     */
    public List<ConfigValueMeta> fields() {
        return fields;
    }

    private static <T> ConfigValueMeta conditionVariantMeta(String typeKey, Variant<T> variant, ConfigValueMeta meta) {
        JsonObject defaultJson = encodeVariantDefault(variant);
        JsonElement defaultValue = ConfigPathUtil.getJsonByPath(defaultJson, meta.key());
        return ConfigValueMeta.builder(meta.key())
                .comment(meta.comment().orElse(null))
                .uiHint(meta.uiHint().orElse(null))
                .translationKey(meta.translationKey().orElse(null))
                .tooltip(meta.tooltip().orElse(null))
                .hidden(meta.hidden())
                .visibleWhenPath(typeKey)
                .visibleWhenValue(variant.id())
                .defaultJsonValue(defaultValue)
                .validators(meta.validators())
                .migrations(meta.migrations())
                .build();
    }

    private static <T> JsonObject encodeVariantDefault(Variant<T> variant) {
        JsonElement encoded = variant.codec().encodeStart(JsonOps.INSTANCE, variant.codec().defaultValue())
                .result()
                .orElseGet(JsonObject::new);
        return encoded.isJsonObject() ? encoded.getAsJsonObject() : new JsonObject();
    }

    @Override
    public <O> DataResult<Pair<T, O>> decode(DynamicOps<O> ops, O input) {
        return delegate.decode(ops, input);
    }

    @Override
    public <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
        return delegate.encode(input, ops, prefix);
    }

    @Override
    public @NotNull String toString() {
        return "ConfigSealedCodec[" + delegate + "]";
    }

    /**
     * Declared sealed variant.
     *
     * @param id the discriminator id
     * @param variantClass the concrete variant class
     * @param codec the metadata codec for the concrete variant
     * @param <T> the concrete variant type
     */
    public record Variant<T>(String id, Class<T> variantClass, ConfigMetaCodec<T> codec) {
        public Variant {
            Objects.requireNonNull(id);
            Objects.requireNonNull(variantClass);
            Objects.requireNonNull(codec);
        }

        MapCodec<T> mapCodec() {
            return MapCodec.assumeMapUnsafe(codec);
        }
    }
}
