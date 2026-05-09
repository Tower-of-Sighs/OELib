package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.util.ConfigCodecUtil;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Codec wrapper that also carries per-field metadata for a record schema.
 *
 * <p>A {@code ConfigMetaCodec} is intended for reusable nested record
 * declarations. It preserves the underlying {@link Codec} contract, while
 * exposing the derived default value and the collected {@link ConfigValueMeta}
 * entries so a parent {@link ConfigSchema} can merge nested metadata into the
 * active configuration context.
 *
 * @param <T> the decoded value type
 */
public final class ConfigMetaCodec<T> implements Codec<T> {
    private static final Identifier META_CONFIG_ID = Identifier.fromNamespaceAndPath(OELib.MODID, "meta_codec");

    private final Codec<T> delegate;
    private final T defaultValue;
    private final List<ConfigValueMeta> fields;

    private ConfigMetaCodec(Codec<T> delegate, T defaultValue, List<ConfigValueMeta> fields) {
        this.delegate = delegate;
        this.defaultValue = defaultValue;
        this.fields = List.copyOf(fields);
    }

    /**
     * Creates a metadata codec from a record codec builder function.
     *
     * <p>The builder executes in an isolated schema context. Nested calls to
     * {@link ConfigSchema#record(String, Class, Function, Function)} and
     * {@link ConfigSchema#record(String, Class, ConfigMetaCodec, Function)}
     * contribute metadata into this codec.
     *
     * @param recordClass the record class represented by this codec
     * @param builder     the record codec builder function
     * @param <T>         the record type
     * @return a metadata codec that delegates to the built codec
     * @throws NullPointerException if {@code recordClass} or {@code builder} is {@code null}
     */
    public static <T> ConfigMetaCodec<T> of(
            Class<T> recordClass,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(builder);
        List<ConfigValueMeta> metas = new ArrayList<>();
        Codec<T> codec = ConfigContext.withRoot(META_CONFIG_ID, recordClass, metas, () -> RecordCodecBuilder.create(builder));
        T defaultValue = ConfigCodecUtil.deriveDefault(codec);
        return new ConfigMetaCodec<>(codec, defaultValue, metas);
    }

    /**
     * Returns the derived default value for this codec.
     *
     * @return the default value
     */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Returns the field metadata collected when this codec was created.
     *
     * @return an unmodifiable list of metadata entries
     */
    public List<ConfigValueMeta> fields() {
        return fields;
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
        return "ConfigMetaCodec[" + delegate + "]";
    }
}
