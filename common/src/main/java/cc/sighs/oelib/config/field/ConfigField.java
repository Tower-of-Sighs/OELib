package cc.sighs.oelib.config.field;

import cc.sighs.oelib.config.ConfigContext;
import cc.sighs.oelib.config.codecs.ConfigSealedCodec;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.ui.ConfigUiHint;
import cc.sighs.oelib.config.util.ConfigFieldMetaUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;
import java.util.function.Function;

/**
 * Static factory for field builders used inside
 * {@link cc.sighs.oelib.config.ConfigSchema ConfigSchema} definitions.
 *
 * <p>Each factory method returns a specialized builder that accumulates
 * metadata and finally produces a {@link RecordCodecBuilder} entry via
 * {@link BaseFieldBuilder#forGetter(Function)}.
 *
 * <p>Example usage:
 * <pre>{@code
 * ConfigField.intRange("port", 1, 65535)
 *     .comment("The listening port")
 *     .tooltip()
 *     .forGetter(MyConfig::port)
 * }</pre>
 */
public final class ConfigField {
    private ConfigField() {
    }

    /**
     * Records field metadata into the active {@link ConfigContext}.
     *
     * @param meta the field metadata to record
     * @throws IllegalStateException if no {@link ConfigContext} is active
     * @throws NullPointerException  if {@code meta} is {@code null}
     */
    public static void recordMeta(ConfigValueMeta meta) {
        if (!ConfigContext.isActive()) {
            throw new IllegalStateException("Config field metadata can only be recorded during schema definition.");
        }
        ConfigContext.recordMeta(meta);
    }

    /**
     * Creates a builder for an integer field constrained to the given range.
     *
     * @param key the field key
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a new integer field builder
     */
    public static IntBuilder intRange(String key, int min, int max) {
        return new IntBuilder(key, Codec.intRange(min, max), min, max, 1);
    }

    /**
     * Creates a builder for a double field constrained to the given range.
     *
     * @param key the field key
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a new double field builder
     */
    public static DoubleBuilder doubleRange(String key, double min, double max) {
        return new DoubleBuilder(key, Codec.doubleRange(min, max), min, max, 0.01);
    }

    /**
     * Creates a builder for a boolean field.
     *
     * @param key the field key
     * @return a new boolean field builder
     */
    public static BoolBuilder bool(String key) {
        return new BoolBuilder(key, Codec.BOOL);
    }

    /**
     * Creates a builder for a string field.
     *
     * @param key the field key
     * @return a new string field builder
     */
    public static StringBuilder string(String key) {
        return new StringBuilder(key, Codec.STRING);
    }

    /**
     * Creates a builder for an enum field.
     *
     * @param key       the field key
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return a new enum field builder
     * @throws NullPointerException if {@code enumClass} is {@code null}
     */
    public static <E extends Enum<E>> EnumBuilder<E> enumValue(String key, Class<E> enumClass) {
        Objects.requireNonNull(enumClass);
        Codec<E> codec = Codec.STRING.xmap(name -> Enum.valueOf(enumClass, name), Enum::name);
        return new EnumBuilder<>(key, codec, enumClass);
    }

    /**
     * Creates a builder for a passthrough dynamic field.
     *
     * @param key the field key
     * @return a new dynamic field builder
     */
    public static DynamicBuilder dynamic(String key) {
        return new DynamicBuilder(key, Codec.PASSTHROUGH);
    }

    /**
     * Creates a builder for a list field.
     *
     * @param key          the field key
     * @param elementCodec the codec for list elements
     * @param <T>          the element type
     * @return a new list field builder
     */
    public static <T> ListBuilder<T> list(String key, Codec<T> elementCodec) {
        return new ListBuilder<>(key, elementCodec);
    }

    /**
     * Creates a builder for a map field.
     *
     * @param key       the field key
     * @param keyCodec  the codec for map keys
     * @param valueCodec the codec for map values
     * @param <K>       the key type
     * @param <V>       the value type
     * @return a new map field builder
     */
    public static <K, V> MapBuilder<K, V> map(String key, Codec<K> keyCodec, Codec<V> valueCodec) {
        return new MapBuilder<>(key, keyCodec, valueCodec);
    }

    /**
     * Creates a builder for an optional field.
     *
     * @param key          the field key
     * @param elementCodec the codec for the inner value
     * @param <T>          the inner value type
     * @return a new optional field builder
     */
    public static <T> OptionalBuilder<T> optional(String key, Codec<T> elementCodec) {
        return new OptionalBuilder<>(key, elementCodec);
    }

    /**
     * Creates a builder for a discriminated sealed field.
     *
     * @param key the field key
     * @param sealedCodec the sealed codec carrying variant metadata
     * @param <T> the sealed base type
     * @return a new sealed field builder
     */
    public static <T> SealedBuilder<T> sealed(String key, ConfigSealedCodec<T> sealedCodec) {
        return new SealedBuilder<>(key, sealedCodec);
    }

    /**
     * Builder for {@code int} fields with slider default UI.
     */
    public static final class IntBuilder extends BaseFieldBuilder<Integer, IntBuilder> {
        private final int min;
        private final int max;
        private final double step;

        public IntBuilder(String key, Codec<Integer> codec, int min, int max, double step) {
            super(key, codec);
            this.min = min;
            this.max = max;
            this.step = step;
            this.metaBuilder.uiHint(ConfigUiHint.slider(min, max, step));
        }

        /**
         * Switches the UI hint to a text field.
         *
         * @return this builder
         */
        public IntBuilder text() {
            this.metaBuilder.uiHint(ConfigUiHint.text());
            return this;
        }

        /**
         * Switches the UI hint to a slider.
         *
         * @return this builder
         */
        public IntBuilder slider() {
            this.metaBuilder.uiHint(ConfigUiHint.slider(min, max, step));
            return this;
        }
    }

    /**
     * Builder for {@code double} fields with slider default UI.
     */
    public static final class DoubleBuilder extends BaseFieldBuilder<Double, DoubleBuilder> {
        private final double min;
        private final double max;
        private final double step;

        public DoubleBuilder(String key, Codec<Double> codec, double min, double max, double step) {
            super(key, codec);
            this.min = min;
            this.max = max;
            this.step = step;
            this.metaBuilder.uiHint(ConfigUiHint.slider(min, max, step));
        }

        /**
         * Switches the UI hint to a text field.
         *
         * @return this builder
         */
        public DoubleBuilder text() {
            this.metaBuilder.uiHint(ConfigUiHint.text());
            return this;
        }

        /**
         * Switches the UI hint to a slider.
         *
         * @return this builder
         */
        public DoubleBuilder slider() {
            this.metaBuilder.uiHint(ConfigUiHint.slider(min, max, step));
            return this;
        }
    }

    /**
     * Builder for {@code boolean} fields.
     */
    public static final class BoolBuilder extends BaseFieldBuilder<Boolean, BoolBuilder> {
        public BoolBuilder(String key, Codec<Boolean> codec) {
            super(key, codec);
        }
    }

    /**
     * Builder for {@link String} fields.
     */
    public static final class StringBuilder extends BaseFieldBuilder<String, StringBuilder> {
        public StringBuilder(String key, Codec<String> codec) {
            super(key, codec);
        }
    }

    /**
     * Builder for enum fields with dropdown default UI.
     *
     * @param <E> the enum type
     */
    public static final class EnumBuilder<E extends Enum<E>> extends BaseFieldBuilder<E, EnumBuilder<E>> {
        private final Class<E> enumClass;

        public EnumBuilder(String key, Codec<E> codec, Class<E> enumClass) {
            super(key, codec);
            this.enumClass = enumClass;
            List<String> names = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList();
            metaBuilder.uiHint(ConfigUiHint.dropdown(names));
        }
    }

    /**
     * Builder for {@link Dynamic} passthrough fields.
     */
    public static final class DynamicBuilder extends BaseFieldBuilder<Dynamic<?>, DynamicBuilder> {
        public DynamicBuilder(String key, Codec<Dynamic<?>> codec) {
            super(key, codec);
        }
    }

    /**
     * Builder for list fields.
     *
     * @param <T> the element type
     */
    public static final class ListBuilder<T> extends BaseFieldBuilder<List<T>, ListBuilder<T>> {
        private final Codec<T> elementCodec;

        ListBuilder(String key, Codec<T> elementCodec) {
            super(key, Codec.list(elementCodec));
            this.elementCodec = elementCodec;
        }

        @Override
        public <O> RecordCodecBuilder<O, List<T>> forGetter(Function<O, List<T>> getter) {
            var cfgId = ConfigContext.currentConfigId();
            if (cfgId != null) {
                String autoKey = ConfigFieldMetaUtil.autoTranslationKey(cfgId, key);
                metaBuilder.translationKey(autoKey);
                if (this.tooltipEnabled) {
                    metaBuilder.tooltip(autoKey + ".tooltip");
                }
            }
            var meta = ConfigFieldMetaUtil.qualifyForContext(metaBuilder.build());
            recordMeta(meta);
            var field = Codec.list(elementCodec).fieldOf(key);
            if (defaultValue != null) {
                field = field.orElse(defaultValue);
            }
            return field.forGetter(getter);
        }
    }

    /**
     * Builder for map fields.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    public static final class MapBuilder<K, V> extends BaseFieldBuilder<Map<K, V>, MapBuilder<K, V>> {
        private final Codec<K> keyCodec;
        private final Codec<V> valueCodec;

        MapBuilder(String key, Codec<K> keyCodec, Codec<V> valueCodec) {
            super(key, Codec.unboundedMap(keyCodec, valueCodec)
                    .xmap(HashMap::new, m -> m));
            this.keyCodec = keyCodec;
            this.valueCodec = valueCodec;
        }


        @Override
        public <O> RecordCodecBuilder<O, Map<K, V>> forGetter(Function<O, Map<K, V>> getter) {
            var cfgId = ConfigContext.currentConfigId();
            if (cfgId != null) {
                String autoKey = ConfigFieldMetaUtil.autoTranslationKey(cfgId, key);
                metaBuilder.translationKey(autoKey);
                if (this.tooltipEnabled) {
                    metaBuilder.tooltip(autoKey + ".tooltip");
                }
            }
            var meta = ConfigFieldMetaUtil.qualifyForContext(metaBuilder.build());
            recordMeta(meta);
            var field = Codec.unboundedMap(keyCodec, valueCodec).fieldOf(key);
            if (defaultValue != null) {
                field = field.orElse(defaultValue);
            }
            return field.forGetter(getter);
        }
    }

    /**
     * Builder for {@link Optional} fields.
     *
     * @param <T> the inner value type
     */
    public static final class OptionalBuilder<T> extends BaseFieldBuilder<Optional<T>, OptionalBuilder<T>> {
        private final Codec<T> elementCodec;

        OptionalBuilder(String key, Codec<T> elementCodec) {
            super(key, elementCodec.optionalFieldOf(key).codec());
            this.elementCodec = elementCodec;
            this.defaultValue = Optional.empty();
        }

        /**
         * Sets the default state for this optional field.
         *
         * <p>Only {@link Optional#empty()} is supported. A present default is
         * rejected because an absent field and an empty optional share the
         * same serialized representation in the supported config formats.
         *
         * @param  value the default optional state
         * @return this builder
         * @throws IllegalArgumentException if {@code value} is present
         */
        @Override
        public OptionalBuilder<T> defaultValue(Optional<T> value) {
            Objects.requireNonNull(value);
            if (value.isPresent()) {
                throw new IllegalArgumentException(
                        "Optional fields do not support present defaults because absent input " +
                                "and Optional.empty() share the same serialized representation."
                );
            }
            return super.defaultValue(value);
        }

        @Override
        public <O> RecordCodecBuilder<O, Optional<T>> forGetter(Function<O, Optional<T>> getter) {
            Objects.requireNonNull(getter);
            if (beforeMetaHook != null) {
                beforeMetaHook.accept(metaBuilder);
            }
            var cfgId = ConfigContext.currentConfigId();
            if (cfgId != null) {
                String autoKey = ConfigFieldMetaUtil.autoTranslationKey(cfgId, key);
                metaBuilder.translationKey(autoKey);
                if (this.tooltipEnabled) {
                    metaBuilder.tooltip(autoKey + ".tooltip");
                }
            }
            var meta = ConfigFieldMetaUtil.qualifyForContext(metaBuilder.build());
            recordMeta(meta);
            var mc = elementCodec.optionalFieldOf(key);
            if (defaultValue.isPresent()) {
                mc = mc.orElse(defaultValue);
            }
            if (afterMetaHook != null) {
                afterMetaHook.accept(meta);
            }
            return mc.forGetter(getter);
        }
    }

    /**
     * Builder for discriminated sealed fields.
     *
     * @param <T> the sealed base type
     */
    public static final class SealedBuilder<T> extends BaseFieldBuilder<T, SealedBuilder<T>> {
        private final ConfigSealedCodec<T> sealedCodec;

        SealedBuilder(String key, ConfigSealedCodec<T> sealedCodec) {
            super(key, sealedCodec);
            this.sealedCodec = Objects.requireNonNull(sealedCodec);
            this.defaultValue = sealedCodec.defaultValue();
        }

        @Override
        public <O> RecordCodecBuilder<O, T> forGetter(Function<O, T> getter) {
            Objects.requireNonNull(getter);
            if (beforeMetaHook != null) {
                beforeMetaHook.accept(metaBuilder);
            }
            var cfgId = ConfigContext.currentConfigId();
            if (cfgId != null) {
                String autoKey = ConfigFieldMetaUtil.autoTranslationKey(cfgId, key);
                metaBuilder.translationKey(autoKey);
                if (this.tooltipEnabled) {
                    metaBuilder.tooltip(autoKey + ".tooltip");
                }
            }
            // The sealed container itself is not directly editable in the UI.
            // Keep its metadata for whole-value validation, migration, and
            // serialization comments, but hide it from field rendering.
            metaBuilder.hidden(true);
            var meta = ConfigFieldMetaUtil.qualifyForContext(metaBuilder.build());
            recordMeta(meta);
            recordSealedMeta(key, sealedCodec);
            var field = sealedCodec.fieldOf(key).orElse(defaultValue);
            if (afterMetaHook != null) {
                afterMetaHook.accept(meta);
            }
            return field.forGetter(getter);
        }

        private static <T> void recordSealedMeta(String key, ConfigSealedCodec<T> sealedCodec) {
            if (!ConfigContext.isActive()) {
                return;
            }
            ConfigContext.withRecord(key, sealedCodec.baseClass(), () -> {
                var configId = ConfigContext.currentConfigId();
                for (ConfigValueMeta localMeta : sealedCodec.fields()) {
                    recordMeta(ConfigFieldMetaUtil.rewriteNestedMetaForContext(localMeta, configId));
                }
                return null;
            });
        }
    }
}
