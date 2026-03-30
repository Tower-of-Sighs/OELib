package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.api.IConfigPermissionChecker;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Global registry and utilities for configuration units.
 * <p>
 * Provides registration helpers, hot-reload entry points, and remote
 * update encoding/decoding for both JSON and TOML representations.
 * </p>
 */
public final class ConfigManager {

    private static final ThreadLocal<Boolean> UPDATING_FROM_SERVER = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ConfigManager() {
    }

    public static void registerClient(ConfigUnit<?> unit) {
        registerUnit(unit);
    }

    public static void registerServer(ConfigUnit<?> unit, IConfigPermissionChecker permissionChecker) {
        registerUnitServer(unit, permissionChecker);
    }

    public static <T> ConfigUnit<T> register(ConfigCodec<T> codec, T defaultValue) {
        var unit = ConfigUnit.of(codec, defaultValue);
        registerUnit(unit);
        return unit;
    }

    public static void registerUnit(ConfigUnit<?> unit) {
        var meta = unit.meta();
        if (meta.fileName() == null || meta.fileName().isBlank()) {
            OELib.LOGGER.error("Config {} has empty filename; please set meta.fileName()", meta.id());
        }
        if (meta.format() == null) {
            OELib.LOGGER.error("Config {} has null format; please set meta.format()", meta.id());
        }
        if (meta.side() == null) {
            OELib.LOGGER.error("Config {} has null side; please set meta.side()", meta.id());
        }
        OELib.LOGGER.info("Registered config {} from mod {} side {} format {} filename {} directory {}",
                meta.id(), meta.id().getNamespace(), meta.side(), meta.format(), meta.fileName(), meta.directory());
        if (meta.side() == ConfigSide.CLIENT) {
            ClientConfigManager.registerUnit(unit);
        } else if (meta.side() == ConfigSide.SERVER) {
            registerUnitServer(unit, null);
        }
    }

    public static void registerUnitServer(ConfigUnit<?> unit, IConfigPermissionChecker permissionChecker) {
        ServerConfigManager.registerUnit(unit, permissionChecker);
    }

    private static <T> T deriveDefault(Codec<T> codec) {
        var element = new JsonObject();
        var res = codec.parse(JsonOps.INSTANCE, element);
        return res.result().orElseThrow(() -> new IllegalStateException("Missing defaults for config; please specify defaultValue for all fields"));
    }

    public static Optional<ConfigUnit<?>> get(Identifier id) {
        var server = ServerConfigManager.get(id);
        if (server.isPresent()) {
            return server;
        }
        return ClientConfigManager.get(id);
    }

    public static void reload(Identifier id) {
        get(id).ifPresent(ConfigUnit::reload);
    }

    public static void reloadAll() {
        reloadAllServer();
        reloadAllClient();
    }

    public static void reloadAllServer() {
        ServerConfigManager.reloadAll();
    }

    public static void reloadAllClient() {
        ClientConfigManager.reloadAll();
    }

    /**
     * Applies a remote payload (JSON/TOML) to a target config and fires sync events.
     *
     * @param id      config id
     * @param payload encoded config string
     * @param format  payload format
     */
    public static void applyRemoteUpdate(Identifier id, String payload, ConfigStorageFormat format) {
        ServerConfigManager.applyRemoteUpdate(id, payload, format);
    }

    public static boolean isUpdatingFromServer() {
        return UPDATING_FROM_SERVER.get();
    }

    public static void runWithServerUpdate(Runnable runnable) {
        boolean previous = UPDATING_FROM_SERVER.get();
        UPDATING_FROM_SERVER.set(Boolean.TRUE);
        try {
            runnable.run();
        } finally {
            UPDATING_FROM_SERVER.set(previous);
        }
    }

    public static <T> T callWithServerUpdate(Supplier<T> supplier) {
        boolean previous = UPDATING_FROM_SERVER.get();
        UPDATING_FROM_SERVER.set(Boolean.TRUE);
        try {
            return supplier.get();
        } finally {
            UPDATING_FROM_SERVER.set(previous);
        }
    }

    /**
     * Encodes current config value to a textual payload in its configured storage format.
     *
     * @param id config id
     * @return payload + format
     */
    public static Optional<EncodedPayload> encodeToString(Identifier id) {
        return ServerConfigManager.encodeToString(id);
    }

    public record EncodedPayload(ConfigStorageFormat format, String payload) {
    }
}
