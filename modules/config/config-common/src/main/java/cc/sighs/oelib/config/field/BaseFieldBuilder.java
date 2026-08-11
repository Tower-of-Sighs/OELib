package cc.sighs.oelib.config.field;

import cc.sighs.oelib.config.ConfigContext;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.util.ConfigFieldMetaUtil;
import com.flechazo.hkt.business.control.ValidatedNel;
import com.flechazo.hkt.business.util.OptionalOps;
import com.flechazo.optics.LensGetter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Provides common schema options for configuration field builders.
 *
 * <p>Each builder describes one serialized field and produces a record codec entry when
 * {@link #forGetter(LensGetter)} is called. A builder records at most one default value and may
 * record multiple validators and migrations in registration order.
 *
 * @param <T> the type of the field value
 * @param <B> the concrete builder type for fluent return types
 */
@SuppressWarnings({"unchecked", "unused"})
public abstract class BaseFieldBuilder<T, B extends BaseFieldBuilder<T, B>> implements FieldBuilder<T> {
    /** The serialized field name. */
    protected final String key;
    /** The metadata builder associated with the field. */
    protected final ConfigValueMeta.Builder metaBuilder;
    /** The codec used for field values. */
    protected Codec<T> codec;
    /** The value used when the serialized field is absent. */
    protected T defaultValue;
    /** The action invoked before metadata is created, or {@code null}. */
    protected Consumer<ConfigValueMeta.Builder> beforeMetaHook;
    /** The action invoked after metadata is created, or {@code null}. */
    protected Consumer<ConfigValueMeta> afterMetaHook;
    /** Whether the field receives an automatically derived tooltip key. */
    protected boolean tooltipEnabled;

    /**
     * Creates a field builder for the specified key and codec.
     *
     * @param key the nonempty serialized field name
     * @param codec the codec that encodes and decodes field values
     */
    protected BaseFieldBuilder(String key, Codec<T> codec) {
        this.key = key;
        this.codec = codec;
        this.metaBuilder = ConfigValueMeta.builder(key);
    }

    /**
     * Sets the comment associated with the serialized field and generated configuration screen.
     *
     * @param text the comment to associate with the field
     * @return this builder instance
     */
    public B comment(String text) {
        metaBuilder.comment(text);
        return (B) this;
    }

    /**
     * Enables a tooltip key derived from the field translation key.
     *
     * @return this builder instance
     */
    public B tooltip() {
        this.tooltipEnabled = true;
        return (B) this;
    }

    /**
     * Sets the value returned by the field codec when the serialized field is absent.
     *
     * @param value the value to use for an absent field
     * @return this builder instance
     */
    public B defaultValue(T value) {
        this.defaultValue = value;
        return (B) this;
    }

    @Override
    public B validate(String code, Function<T, Optional<String>> validator) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(validator);
        metaBuilder.validator((fieldValue, entireConfig) -> {
            @SuppressWarnings("unchecked")
            T cast = (T) fieldValue;
            return OptionalOps.toMaybe(validator.apply(cast)).fold(
                    () -> ValidatedNel.valid(fieldValue),
                    message -> ValidatedNel.invalid(
                            new ConfigValueMeta.ValidationFailure(code, message)));
        });
        return (B) this;
    }

    @Override
    public <R> B validateRoot(
            String code, Class<R> rootClass, BiFunction<T, R, Optional<String>> validator) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(rootClass, "rootClass");
        Objects.requireNonNull(validator, "validator");
        metaBuilder.validator((fieldValue, entireConfig) -> {
            @SuppressWarnings("unchecked") T cast = (T) fieldValue;
            return OptionalOps.toMaybe(validator.apply(cast, rootClass.cast(entireConfig))).fold(
                    () -> ValidatedNel.valid(fieldValue),
                    message -> ValidatedNel.invalid(
                            new ConfigValueMeta.ValidationFailure(code, message)));
        });
        return (B) this;
    }

    @Override
    public B migrate(
            int fromVersion, int toVersion, UnaryOperator<Dynamic<?>> migration) {
        Objects.requireNonNull(migration);
        metaBuilder.migration(fromVersion, toVersion, migration);
        return (B) this;
    }

    /**
     * Excludes the field from generated configuration screens.
     *
     * @return this builder instance
     */
    public B hiddenInUi() {
        this.metaBuilder.hidden(true);
        return (B) this;
    }

    /**
     * Registers an action invoked immediately before field metadata is created.
     *
     * @param hook the action that may modify the field metadata builder
     * @return this builder instance
     */
    public B beforeMeta(Consumer<ConfigValueMeta.Builder> hook) {
        this.beforeMetaHook = hook;
        return (B) this;
    }

    /**
     * Registers an action invoked after field metadata is created.
     *
     * @param hook the action that receives the completed field metadata
     * @return this builder instance
     */
    public B afterMeta(Consumer<ConfigValueMeta> hook) {
        this.afterMetaHook = hook;
        return (B) this;
    }

    /**
     * Creates a record codec entry and registers the field with the active schema definition.
 *
     * <p>The accessor must identify a component of the record type active in the current
     * {@link ConfigContext}. The returned entry uses the configured default value when present.
 *
     * @param getter the record component accessor associated with this field
     * @param <O> the record type containing the field
     * @return a codec builder entry for the containing record
     * @throws IllegalArgumentException if {@code getter} does not identify a record component
     * @throws IllegalStateException if no schema definition is active
     */
    public <O> RecordCodecBuilder<O, T> forGetter(LensGetter<O, T> getter) {
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
        metaBuilder.valueCodec(codec)
                .defaultValue(defaultValue)
                .accessor(ConfigContext.compileAccessor(getter));
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
