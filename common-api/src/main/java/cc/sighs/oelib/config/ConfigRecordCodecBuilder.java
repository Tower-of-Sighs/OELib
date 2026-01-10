package cc.sighs.oelib.config;

import cc.sighs.oelib.config.api.IConfigPermissionChecker;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds a {@link ConfigUnit} together with collected field metadata.
 * <p>
 * While the given builder constructs the serialization schema, this utility
 * intercepts field definitions and records {@link cc.sighs.oelib.config.model.ConfigValueMeta}
 * for downstream UI and comment generation. It then creates a {@link ConfigUnit}
 * ready to be registered via {@link ConfigManager#registerClient(ConfigUnit)} or
 * {@link ConfigManager#registerServer(ConfigUnit, IConfigPermissionChecker)}.
 * </p>
 */
public class ConfigRecordCodecBuilder {
    private ConfigRecordCodecBuilder() {
    }

    /**
     * Creates a client-side {@link ConfigUnit} by invoking {@link RecordCodecBuilder}.
     *
     * @param configId       textual id in the form "namespace:path"
     * @param builder        codec builder using the standard group/apply pattern
     * @param metaCustomizer optional customizer for {@link ConfigMeta} (filename, format, directory)
     */
    public static <T> ConfigUnit<T> createClient(
            ResourceLocation configId,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            Consumer<ConfigMeta.Builder> metaCustomizer
    ) {
        return createInternal(configId, builder, metaCustomizer, ConfigSide.CLIENT);
    }

    /**
     * Creates a server-side {@link ConfigUnit} by invoking {@link RecordCodecBuilder}.
     *
     * @param configId       textual id in the form "namespace:path"
     * @param builder        codec builder using the standard group/apply pattern
     * @param metaCustomizer optional customizer for {@link ConfigMeta} (filename, format, directory)
     */
    public static <T> ConfigUnit<T> create(
            ResourceLocation configId,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            Consumer<ConfigMeta.Builder> metaCustomizer
    ) {
        return createInternal(configId, builder, metaCustomizer, ConfigSide.SERVER);
    }

    /**
     * Internal shared implementation for creating a {@link ConfigUnit}.
     *
     * @param configId       textual id in the form "namespace:path"
     * @param builder        codec builder using the standard group/apply pattern
     * @param metaCustomizer optional customizer for {@link ConfigMeta}
     * @param side           whether this config is for client or server
     */
    private static <T> ConfigUnit<T> createInternal(
            ResourceLocation configId,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            Consumer<ConfigMeta.Builder> metaCustomizer,
            ConfigSide side
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

        var metaBuilder = ConfigMeta.builder(configId)
                .format(ConfigStorageFormat.TOML)
                .side(side);
        if (metaCustomizer != null) {
            metaCustomizer.accept(metaBuilder);
        }
        ConfigMeta meta = metaBuilder.build();
        ConfigCodec<T> configCodec = new ConfigCodec<>(codec, meta, List.copyOf(fields));
        T defaultValue = deriveDefault(codec);
        return ConfigUnit.of(configCodec, defaultValue);
    }

    private static <T> T deriveDefault(Codec<T> codec) {
        var element = new com.google.gson.JsonObject();
        var res = codec.parse(com.mojang.serialization.JsonOps.INSTANCE, element);
        return res.result().orElseThrow(() -> new IllegalStateException("Missing defaults for config; please specify defaultValue for all fields"));
    }
}