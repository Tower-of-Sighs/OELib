package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Builds a {@link Codec} along with collected field metadata.
 * <p>
 * While the given builder constructs the serialization schema, this utility
 * intercepts field definitions and records {@link cc.sighs.oelib.config.model.ConfigValueMeta}
 * for downstream UI and comment generation.
 * </p>
 */
public class ConfigRecordCodecBuilder {
    private ConfigRecordCodecBuilder() {
    }

    /**
     * Creates a {@link ConfigCodec} by invoking {@link RecordCodecBuilder}.
     *
     * @param configId textual id in the form "namespace:path"
     * @param builder  codec builder using the standard group/apply pattern
     */
    public static <T> ConfigCodec<T> create(
            ResourceLocation configId,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder
    ) {
        Objects.requireNonNull(configId);
        Objects.requireNonNull(builder);
        List<ConfigValueMeta> fields = new ArrayList<>();
        ConfigField.CURRENT_FIELDS.set(fields);
        ConfigField.CURRENT_CONFIG_ID.set(configId);
        Codec<T> codec;
        try {
            codec = RecordCodecBuilder.create(builder);
        } finally {
            ConfigField.CURRENT_FIELDS.remove();
            ConfigField.CURRENT_CONFIG_ID.remove();
        }
        ConfigMeta meta = ConfigMeta.builder(configId)
                .format(ConfigStorageFormat.TOML)
                .build();
        return new ConfigCodec<>(codec, meta, List.copyOf(fields));
    }
}
