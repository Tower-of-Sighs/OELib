package cc.sighs.oelib.config.field;

import cc.sighs.oelib.config.ConfigContext;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.util.ConfigFieldMetaUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Abstract base for field builders providing shared metadata accumulation
 * and codec wiring.
 *
 * <p>Subclasses are created via the static factory methods on
 * {@link ConfigField} (for example {@link ConfigField#intRange
 * ConfigField.intRange}). Each subclass overrides {@link #forGetter(Function)}
 * to finalize the field and return a {@link RecordCodecBuilder} entry.
 *
 * @param <T> the type of the field value
 * @param <B> the concrete builder type for fluent return types
 */
@SuppressWarnings({"unchecked", "unused"})
public abstract class BaseFieldBuilder<T, B extends BaseFieldBuilder<T, B>> implements FieldBuilder<T> {
    protected final String key;
    protected final ConfigValueMeta.Builder metaBuilder;
    protected Codec<T> codec;
    protected T defaultValue;
    protected Consumer<ConfigValueMeta.Builder> beforeMetaHook;
    protected Consumer<ConfigValueMeta> afterMetaHook;
    protected Component validatorDescription;
    protected boolean tooltipEnabled;

    /**
     * Constructs a base field builder.
     *
     * @param key   the field key
     * @param codec the Mojang codec for this field's type
     */
    protected BaseFieldBuilder(String key, Codec<T> codec) {
        this.key = key;
        this.codec = codec;
        this.metaBuilder = ConfigValueMeta.builder(key);
    }

    /**
     * Sets the comment text displayed in TOML/JSON5 output and in the UI.
     *
     * @param text the comment text
     * @return this builder
     */
    public B comment(String text) {
        metaBuilder.comment(text);
        return (B) this;
    }

    /**
     * Enables automatic tooltip generation from the translation key.
     *
     * @return this builder
     */
    public B tooltip() {
        this.tooltipEnabled = true;
        return (B) this;
    }

    /**
     * Sets the default value used when decoding an absent field.
     *
     * @param value the default value
     * @return this builder
     */
    public B defaultValue(T value) {
        this.defaultValue = value;
        return (B) this;
    }

    @Override
    public B validate(BiFunction<T, Object, Optional<String>> validator) {
        Objects.requireNonNull(validator);
        metaBuilder.validator((fieldValue, entireConfig) -> {
            @SuppressWarnings("unchecked")
            T cast = (T) fieldValue;
            return validator.apply(cast, entireConfig);
        });
        return (B) this;
    }

    @Override
    public B migrate(int version, UnaryOperator<Dynamic<?>> migration) {
        Objects.requireNonNull(migration);
        metaBuilder.migration(version, migration);
        return (B) this;
    }

    /**
     * Marks this field as hidden in generated configuration screens.
     *
     * @return this builder
     */
    public B hiddenInUi() {
        this.metaBuilder.hidden(true);
        return (B) this;
    }

    /**
     * Registers a hook that runs before the metadata is finalized.
     *
     * @param hook a consumer that receives the metadata builder
     * @return this builder
     */
    public B beforeMeta(Consumer<ConfigValueMeta.Builder> hook) {
        this.beforeMetaHook = hook;
        return (B) this;
    }

    /**
     * Registers a hook that runs after the metadata is finalized.
     *
     * @param hook a consumer that receives the built metadata
     * @return this builder
     */
    public B afterMeta(Consumer<ConfigValueMeta> hook) {
        this.afterMetaHook = hook;
        return (B) this;
    }

    /**
     * Finalizes this field and produces a record codec builder entry.
     *
     * <p>If a {@link ConfigContext} is active, translation keys are
     * automatically derived and the metadata key is qualified for the
     * current nesting depth.
     *
     * @param getter the accessor function on the parent record
     * @param <O>    the parent record type
     * @return a record codec builder for this field
     * @throws NullPointerException if {@code getter} is {@code null}
     */
    public <O> RecordCodecBuilder<O, T> forGetter(Function<O, T> getter) {
        Objects.requireNonNull(getter);
        if (beforeMetaHook != null) {
            beforeMetaHook.accept(metaBuilder);
        }
        var cfgId = ConfigContext.currentConfigId();
        if (cfgId != null) {
            String autoKey = ConfigFieldMetaUtil.autoTranslationKey(cfgId, key);
            metaBuilder.translationKey(autoKey);
            if (tooltipEnabled) {
                metaBuilder.tooltip(autoKey + ".tooltip");
            }
        }
        var meta = ConfigFieldMetaUtil.qualifyForContext(metaBuilder.build());
        ConfigField.recordMeta(meta);
        var field = codec.fieldOf(key);
        if (defaultValue != null) {
            field = field.orElse(defaultValue);
        }
        if (afterMetaHook != null) {
            afterMetaHook.accept(meta);
        }
        return field.forGetter(getter);
    }
}
