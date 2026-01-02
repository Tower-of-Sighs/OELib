package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.api.ConfigEvents;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Global registry and utilities for configuration units.
 * <p>
 * Provides registration helpers, hot-reload entry points, and remote
 * update encoding/decoding for both JSON and TOML representations.
 * </p>
 */
public final class ConfigManager {
    private static final Map<ResourceLocation, ConfigUnit<?>> CONFIGS = new ConcurrentHashMap<>();

    private ConfigManager() {
    }

    /**
     * Simplified registration helper using group/apply codec style and a meta customizer.
     *
     * @param configId       textual id "namespace:path"
     * @param codecBuilder   RecordCodecBuilder group/apply builder
     * @param defaultValue   default object to use when file does not exist
     * @param metaCustomizer optional customizer for {@link ConfigMeta} (filename, format, side, permission, directory)
     */
    public static <T> ConfigUnit<T> register(
            ResourceLocation configId,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> codecBuilder,
            T defaultValue,
            Consumer<ConfigMeta.Builder> metaCustomizer
    ) {
        var base = ConfigRecordCodecBuilder.create(configId, codecBuilder);
        var builder = ConfigMeta.builder(base.meta().id());
        if (metaCustomizer != null) {
            metaCustomizer.accept(builder);
        }
        ConfigCodec<T> finalCodec = new ConfigCodec<>(base.codec(), builder.build(), base.fields());
        return register(finalCodec, defaultValue);
    }

    public static <T> ConfigUnit<T> register(ConfigCodec<T> codec, T defaultValue) {
        var unit = ConfigUnit.of(codec, defaultValue);
        registerUnit(unit);
        return unit;
    }

    public static void registerUnit(ConfigUnit<?> unit) {
        CONFIGS.put(unit.id(), unit);
        if (unit.meta().side().equals(ConfigSide.CLIENT) && unit.codec().fields() != null) {
            boolean hasPerm = unit.codec().fields().stream().anyMatch(f -> f.permissionLevel() > 0);
            if (hasPerm) {
                OELib.LOGGER.warn("Client-side config {} contains permissionLevel on fields, which will be ignored", unit.id());
            }
        }
    }

    public static Optional<ConfigUnit<?>> get(ResourceLocation id) {
        return Optional.ofNullable(CONFIGS.get(id));
    }

    public static void reload(ResourceLocation id) {
        var unit = CONFIGS.get(id);
        if (unit != null) {
            unit.reload();
        }
    }

    public static void reloadAll() {
        for (ConfigUnit<?> unit : CONFIGS.values()) {
            unit.reload();
        }
    }

    /**
     * Applies a remote payload (JSON/TOML) to a target config and fires sync events.
     *
     * @param id      config id
     * @param payload encoded config string
     * @param format  payload format
     */
    public static void applyRemoteUpdate(ResourceLocation id, String payload, ConfigStorageFormat format) {
        var unit = CONFIGS.get(id);
        if (unit == null) {
            return;
        }
        applyRemoteUpdate1(unit, payload, format);
    }

    private static <T> void applyRemoteUpdate1(ConfigUnit<T> unit, String payload, ConfigStorageFormat format) {
        DataResult<T> result = ConfigSerializationUtil.parse(payload, format, unit.codec().codec());
        if (result.error().isPresent()) {
            OELib.LOGGER.error("Failed to apply remote config {}: {}", unit.id(), result.error().get().message());
            return;
        }
        result.result().ifPresent(v -> {
            unit.setValue(v);
            ConfigEvents.onSync(unit, v, true);
        });
    }

    /**
     * Encodes current config value to a textual payload in its configured storage format.
     *
     * @param id config id
     * @return payload + format
     */
    public static Optional<EncodedPayload> encodeToString(ResourceLocation id) {
        var unit = CONFIGS.get(id);
        if (unit == null) {
            return Optional.empty();
        }
        return encodeToString0(unit);
    }

    private static <T> Optional<EncodedPayload> encodeToString0(ConfigUnit<T> unit) {
        var format = unit.meta().format();
        var encoded = ConfigSerializationUtil.encodeToString(unit.get(), format, unit.codec().codec(), unit.codec().fields());
        return encoded.map(s -> new EncodedPayload(format, s));
    }

    public record EncodedPayload(ConfigStorageFormat format, String payload) {
    }
}
