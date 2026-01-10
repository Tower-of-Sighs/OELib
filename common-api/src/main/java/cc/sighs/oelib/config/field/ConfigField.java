package cc.sighs.oelib.config.field;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.ui.ConfigUiHint;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Function;

public final class ConfigField {
    public static final ThreadLocal<List<ConfigValueMeta>> CURRENT_FIELDS = new ThreadLocal<>();
    public static final ThreadLocal<ResourceLocation> CURRENT_CONFIG_ID = new ThreadLocal<>();

    private ConfigField() {
    }

    public static void recordMeta(ConfigValueMeta meta) {
        List<ConfigValueMeta> list = CURRENT_FIELDS.get();
        if (list != null) {
            list.add(meta);
        }
    }

    public static IntBuilder intRange(String key, int min, int max) {
        return new IntBuilder(key, Codec.intRange(min, max), min, max, 1);
    }

    public static DoubleBuilder doubleRange(String key, double min, double max) {
        return new DoubleBuilder(key, Codec.doubleRange(min, max), min, max, 0.01);
    }

    public static BoolBuilder bool(String key) {
        return new BoolBuilder(key, Codec.BOOL);
    }

    public static StringBuilder string(String key) {
        return new StringBuilder(key, Codec.STRING);
    }

    public static <E extends Enum<E>> EnumBuilder<E> enumValue(String key, Class<E> enumClass) {
        Objects.requireNonNull(enumClass);
        Codec<E> codec = Codec.STRING.xmap(name -> Enum.valueOf(enumClass, name), Enum::name);
        return new EnumBuilder<>(key, codec, enumClass);
    }

    public static DynamicBuilder dynamic(String key) {
        return new DynamicBuilder(key, Codec.PASSTHROUGH);
    }

    public static <T> ListBuilder<T> list(String key, Codec<T> elementCodec) {
        return new ListBuilder<>(key, elementCodec);
    }

    public static <K, V> MapBuilder<K, V> map(String key, Codec<K> keyCodec, Codec<V> valueCodec) {
        return new MapBuilder<>(key, keyCodec, valueCodec);
    }

    public static <T> OptionalBuilder<T> optional(String key, Codec<T> elementCodec) {
        return new OptionalBuilder<>(key, elementCodec);
    }

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

        public IntBuilder text() {
            this.metaBuilder.uiHint(ConfigUiHint.text());
            return this;
        }

        public IntBuilder slider() {
            this.metaBuilder.uiHint(ConfigUiHint.slider(min, max, step));
            return this;
        }
    }

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

        public DoubleBuilder text() {
            this.metaBuilder.uiHint(ConfigUiHint.text());
            return this;
        }

        public DoubleBuilder slider() {
            this.metaBuilder.uiHint(ConfigUiHint.slider(min, max, step));
            return this;
        }
    }

    public static final class BoolBuilder extends BaseFieldBuilder<Boolean, BoolBuilder> {
        public BoolBuilder(String key, Codec<Boolean> codec) {
            super(key, codec);
        }
    }

    public static final class StringBuilder extends BaseFieldBuilder<String, StringBuilder> {
        public StringBuilder(String key, Codec<String> codec) {
            super(key, codec);
        }
    }

    public static final class EnumBuilder<E extends Enum<E>> extends BaseFieldBuilder<E, EnumBuilder<E>> {
        private final Class<E> enumClass;

        public EnumBuilder(String key, Codec<E> codec, Class<E> enumClass) {
            super(key, codec);
            this.enumClass = enumClass;
            List<String> names = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList();
            metaBuilder.uiHint(ConfigUiHint.dropdown(names));
        }
    }

    public static final class DynamicBuilder extends BaseFieldBuilder<Dynamic<?>, DynamicBuilder> {
        public DynamicBuilder(String key, Codec<Dynamic<?>> codec) {
            super(key, codec);
        }
    }

    public static final class ListBuilder<T> extends BaseFieldBuilder<List<T>, ListBuilder<T>> {
        private final Codec<T> elementCodec;

        ListBuilder(String key, Codec<T> elementCodec) {
            super(key, Codec.list(elementCodec));
            this.elementCodec = elementCodec;
        }

        @Override
        public <O> RecordCodecBuilder<O, List<T>> forGetter(Function<O, List<T>> getter) {
            var cfgId = CURRENT_CONFIG_ID.get();
            if (cfgId != null) {
                String autoKey = "config." + cfgId.getNamespace() + "." + cfgId.getPath() + "." + key;
                metaBuilder.translationKey(autoKey);
                if (this.tooltipEnabled) {
                    metaBuilder.tooltip(autoKey + ".tooltip");
                }
            }
            var meta = metaBuilder.build();
            recordMeta(meta);
            var field = Codec.list(elementCodec).fieldOf(meta.key());
            if (defaultValue != null) {
                field = field.orElse(defaultValue);
            }
            return field.forGetter(getter);
        }
    }

    public static final class MapBuilder<K, V> extends BaseFieldBuilder<Map<K, V>, MapBuilder<K, V>> {
        private final Codec<K> keyCodec;
        private final Codec<V> valueCodec;

        MapBuilder(String key, Codec<K> keyCodec, Codec<V> valueCodec) {
            super(key, Codec.unboundedMap(keyCodec, valueCodec)
                    .xmap(java.util.HashMap::new, m -> m));
            this.keyCodec = keyCodec;
            this.valueCodec = valueCodec;
        }


        @Override
        public <O> RecordCodecBuilder<O, Map<K, V>> forGetter(Function<O, Map<K, V>> getter) {
            var cfgId = CURRENT_CONFIG_ID.get();
            if (cfgId != null) {
                String autoKey = "config." + cfgId.getNamespace() + "." + cfgId.getPath() + "." + key;
                metaBuilder.translationKey(autoKey);
                if (this.tooltipEnabled) {
                    metaBuilder.tooltip(autoKey + ".tooltip");
                }
            }
            var meta = metaBuilder.build();
            recordMeta(meta);
            var field = Codec.unboundedMap(keyCodec, valueCodec).fieldOf(meta.key());
            if (defaultValue != null) {
                field = field.orElse(defaultValue);
            }
            return field.forGetter(getter);
        }
    }

    public static final class OptionalBuilder<T> extends BaseFieldBuilder<Optional<T>, OptionalBuilder<T>> {
        private final Codec<T> elementCodec;

        OptionalBuilder(String key, Codec<T> elementCodec) {
            super(key, elementCodec.optionalFieldOf(key).codec());
            this.elementCodec = elementCodec;
            this.defaultValue = Optional.empty();
        }

        @Override
        public <O> RecordCodecBuilder<O, Optional<T>> forGetter(Function<O, Optional<T>> getter) {
            Objects.requireNonNull(getter);
            if (beforeMetaHook != null) {
                beforeMetaHook.accept(metaBuilder);
            }
            var cfgId = CURRENT_CONFIG_ID.get();
            if (cfgId != null) {
                String autoKey = "config." + cfgId.getNamespace() + "." + cfgId.getPath() + "." + key;
                metaBuilder.translationKey(autoKey);
                if (this.tooltipEnabled) {
                    metaBuilder.tooltip(autoKey + ".tooltip");
                }
            }
            var meta = metaBuilder.build();
            recordMeta(meta);
            var mc = elementCodec.optionalFieldOf(meta.key());
            if (defaultValue.isPresent()) {
                mc = mc.orElse(defaultValue);
            }
            if (afterMetaHook != null) {
                afterMetaHook.accept(meta);
            }
            return mc.forGetter(getter);
        }
    }
}
