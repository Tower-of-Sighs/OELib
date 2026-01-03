package cc.sighs.oelib.config;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.ui.ConfigUiHint;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntPredicate;

public final class ConfigField {
    static final ThreadLocal<List<ConfigValueMeta>> CURRENT_FIELDS = new ThreadLocal<>();

    private ConfigField() {
    }

    public static IntBuilder intRange(String key, int min, int max) {
        return new IntBuilder(key, Codec.intRange(min, max));
    }

    public static DoubleBuilder doubleRange(String key, double min, double max) {
        return new DoubleBuilder(key, Codec.doubleRange(min, max));
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
        return new EnumBuilder<>(key, codec);
    }

    public static <T> CustomBuilder<T> custom(String key, Codec<T> codec) {
        Objects.requireNonNull(codec);
        return new CustomBuilder<>(key, codec);
    }

    public static DynamicBuilder dynamic(String key) {
        return new DynamicBuilder(key, Codec.PASSTHROUGH);
    }

    private static void recordMeta(ConfigValueMeta meta) {
        List<ConfigValueMeta> list = CURRENT_FIELDS.get();
        if (list != null) {
            list.add(meta);
        }
    }

    public static final class IntBuilder {
        private final String key;
        private final Codec<Integer> codec;
        private final ConfigValueMeta.Builder metaBuilder;
        private Integer defaultValue;
        private IntPredicate validator;

        IntBuilder(String key, Codec<Integer> codec) {
            this.key = key;
            this.codec = codec;
            this.metaBuilder = ConfigValueMeta.builder(key);
        }

        public IntBuilder comment(String text) {
            metaBuilder.comment(text);
            return this;
        }

        public IntBuilder ui(ConfigUiHint ui) {
            metaBuilder.uiHint(ui);
            return this;
        }

        public IntBuilder defaultValue(int value) {
            this.defaultValue = value;
            return this;
        }

        public IntBuilder permissionLevel(int level) {
            return this;
        }

        public IntBuilder validator(IntPredicate predicate) {
            this.validator = predicate;
            return this;
        }

        public <O> RecordCodecBuilder<O, Integer> forGetter(Function<O, Integer> getter) {
            Objects.requireNonNull(getter);
            ConfigValueMeta meta = metaBuilder.build();
            recordMeta(meta);
            return codec.fieldOf(meta.key()).forGetter(getter);
        }

        public Integer defaultValue() {
            return defaultValue;
        }

        public IntPredicate validator() {
            return validator;
        }
    }

    public static final class DoubleBuilder {
        private final String key;
        private final Codec<Double> codec;
        private final ConfigValueMeta.Builder metaBuilder;
        private Double defaultValue;

        DoubleBuilder(String key, Codec<Double> codec) {
            this.key = key;
            this.codec = codec;
            this.metaBuilder = ConfigValueMeta.builder(key);
        }

        public DoubleBuilder comment(String text) {
            metaBuilder.comment(text);
            return this;
        }

        public DoubleBuilder ui(ConfigUiHint ui) {
            metaBuilder.uiHint(ui);
            return this;
        }

        public DoubleBuilder defaultValue(double value) {
            this.defaultValue = value;
            return this;
        }

        public DoubleBuilder permissionLevel(int level) {
            return this;
        }

        public <O> RecordCodecBuilder<O, Double> forGetter(Function<O, Double> getter) {
            Objects.requireNonNull(getter);
            ConfigValueMeta meta = metaBuilder.build();
            recordMeta(meta);
            return codec.fieldOf(meta.key()).forGetter(getter);
        }

        public Double defaultValue() {
            return defaultValue;
        }
    }

    public static final class BoolBuilder {
        private final String key;
        private final Codec<Boolean> codec;
        private final ConfigValueMeta.Builder metaBuilder;
        private Boolean defaultValue;

        BoolBuilder(String key, Codec<Boolean> codec) {
            this.key = key;
            this.codec = codec;
            this.metaBuilder = ConfigValueMeta.builder(key);
        }

        public BoolBuilder comment(String text) {
            metaBuilder.comment(text);
            return this;
        }

        public BoolBuilder ui(ConfigUiHint ui) {
            metaBuilder.uiHint(ui);
            return this;
        }

        public BoolBuilder defaultValue(boolean value) {
            this.defaultValue = value;
            return this;
        }

        public BoolBuilder permissionLevel(int level) {
            return this;
        }

        public <O> RecordCodecBuilder<O, Boolean> forGetter(Function<O, Boolean> getter) {
            Objects.requireNonNull(getter);
            ConfigValueMeta meta = metaBuilder.build();
            recordMeta(meta);
            return codec.fieldOf(meta.key()).forGetter(getter);
        }

        public Boolean defaultValue() {
            return defaultValue;
        }
    }

    public static final class StringBuilder {
        private final String key;
        private final Codec<String> codec;
        private final ConfigValueMeta.Builder metaBuilder;
        private String defaultValue;

        StringBuilder(String key, Codec<String> codec) {
            this.key = key;
            this.codec = codec;
            this.metaBuilder = ConfigValueMeta.builder(key);
        }

        public StringBuilder comment(String text) {
            metaBuilder.comment(text);
            return this;
        }

        public StringBuilder ui(ConfigUiHint ui) {
            metaBuilder.uiHint(ui);
            return this;
        }

        public StringBuilder defaultValue(String value) {
            this.defaultValue = value;
            return this;
        }

        public StringBuilder permissionLevel(int level) {
            return this;
        }

        public <O> RecordCodecBuilder<O, String> forGetter(Function<O, String> getter) {
            Objects.requireNonNull(getter);
            ConfigValueMeta meta = metaBuilder.build();
            recordMeta(meta);
            return codec.fieldOf(meta.key()).forGetter(getter);
        }

        public String defaultValue() {
            return defaultValue;
        }
    }

    public static final class EnumBuilder<E extends Enum<E>> {
        private final String key;
        private final Codec<E> codec;
        private final ConfigValueMeta.Builder metaBuilder;
        private E defaultValue;

        EnumBuilder(String key, Codec<E> codec) {
            this.key = key;
            this.codec = codec;
            this.metaBuilder = ConfigValueMeta.builder(key);
        }

        public EnumBuilder<E> comment(String text) {
            metaBuilder.comment(text);
            return this;
        }

        public EnumBuilder<E> ui(ConfigUiHint ui) {
            metaBuilder.uiHint(ui);
            return this;
        }

        public EnumBuilder<E> defaultValue(E value) {
            this.defaultValue = value;
            return this;
        }

        public EnumBuilder<E> permissionLevel(int level) {
            return this;
        }

        public <O> RecordCodecBuilder<O, E> forGetter(Function<O, E> getter) {
            Objects.requireNonNull(getter);
            ConfigValueMeta meta = metaBuilder.build();
            recordMeta(meta);
            return codec.fieldOf(meta.key()).forGetter(getter);
        }

        public E defaultValue() {
            return defaultValue;
        }
    }

    public static final class CustomBuilder<T> {
        private final String key;
        private final Codec<T> codec;
        private final ConfigValueMeta.Builder metaBuilder;
        private T defaultValue;

        CustomBuilder(String key, Codec<T> codec) {
            this.key = key;
            this.codec = codec;
            this.metaBuilder = ConfigValueMeta.builder(key);
        }

        public CustomBuilder<T> comment(String text) {
            metaBuilder.comment(text);
            return this;
        }

        public CustomBuilder<T> ui(ConfigUiHint ui) {
            metaBuilder.uiHint(ui);
            return this;
        }

        public CustomBuilder<T> defaultValue(T value) {
            this.defaultValue = value;
            return this;
        }

        public CustomBuilder<T> permissionLevel(int level) {
            return this;
        }

        public <O> RecordCodecBuilder<O, T> forGetter(Function<O, T> getter) {
            Objects.requireNonNull(getter);
            ConfigValueMeta meta = metaBuilder.build();
            recordMeta(meta);
            return codec.fieldOf(meta.key()).forGetter(getter);
        }

        public T defaultValue() {
            return defaultValue;
        }
    }

    public static final class DynamicBuilder {
        private final String key;
        private final Codec<Dynamic<?>> codec;
        private final ConfigValueMeta.Builder metaBuilder;
        private Dynamic<?> defaultValue;

        DynamicBuilder(String key, Codec<Dynamic<?>> codec) {
            this.key = key;
            this.codec = codec;
            this.metaBuilder = ConfigValueMeta.builder(key);
        }

        public DynamicBuilder comment(String text) {
            metaBuilder.comment(text);
            return this;
        }

        public DynamicBuilder ui(ConfigUiHint ui) {
            metaBuilder.uiHint(ui);
            return this;
        }

        public DynamicBuilder defaultValue(Dynamic<?> value) {
            this.defaultValue = value;
            return this;
        }

        public DynamicBuilder permissionLevel(int level) {
            return this;
        }

        public <O> RecordCodecBuilder<O, Dynamic<?>> forGetter(Function<O, Dynamic<?>> getter) {
            Objects.requireNonNull(getter);
            ConfigValueMeta meta = metaBuilder.build();
            recordMeta(meta);
            return codec.fieldOf(meta.key()).forGetter(getter);
        }

        public Dynamic<?> defaultValue() {
            return defaultValue;
        }
    }
}
