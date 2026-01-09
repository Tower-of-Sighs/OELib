package cc.sighs.oelib.config.field;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "unused"})
public abstract class BaseFieldBuilder<T, B extends BaseFieldBuilder<T, B>> implements FieldBuilder<T> {
    protected final String key;
    protected final ConfigValueMeta.Builder metaBuilder;
    protected Codec<T> codec;
    protected T defaultValue;
    protected Consumer<ConfigValueMeta.Builder> beforeMetaHook;
    protected Consumer<ConfigValueMeta> afterMetaHook;
    protected Component validatorDescription;

    protected BaseFieldBuilder(String key, Codec<T> codec) {
        this.key = key;
        this.codec = codec;
        this.metaBuilder = ConfigValueMeta.builder(key);
    }

    public B comment(String text) {
        metaBuilder.comment(text);
        return (B) this;
    }

    public B tooltip(String text) {
        metaBuilder.tooltip(text);
        return (B) this;
    }

    public B defaultValue(T value) {
        this.defaultValue = value;
        return (B) this;
    }

    public B hiddenInUi() {
        this.metaBuilder.hidden(true);
        return (B) this;
    }

    public B beforeMeta(Consumer<ConfigValueMeta.Builder> hook) {
        this.beforeMetaHook = hook;
        return (B) this;
    }

    public B afterMeta(Consumer<ConfigValueMeta> hook) {
        this.afterMetaHook = hook;
        return (B) this;
    }

    public <O> RecordCodecBuilder<O, T> forGetter(Function<O, T> getter) {
        Objects.requireNonNull(getter);
        if (beforeMetaHook != null) {
            beforeMetaHook.accept(metaBuilder);
        }
        var cfgId = ConfigField.CURRENT_CONFIG_ID.get();
        if (cfgId != null && metaBuilder != null) {
            String autoKey = "config." + cfgId.getNamespace() + "." + cfgId.getPath() + "." + key;
            metaBuilder.translationKey(autoKey);
            metaBuilder.tooltip(autoKey + ".tooltip");
        }
        var meta = metaBuilder.build();
        ConfigField.recordMeta(meta);
        var field = codec.fieldOf(meta.key());
        if (defaultValue != null) {
            field = field.orElse(defaultValue);
        }
        if (afterMetaHook != null) {
            afterMetaHook.accept(meta);
        }
        return field.forGetter(getter);
    }
}
