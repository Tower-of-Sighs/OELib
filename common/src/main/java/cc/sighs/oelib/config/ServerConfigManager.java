package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.api.ConfigEvents;
import cc.sighs.oelib.config.api.IConfigPermissionChecker;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.net.ConfigSyncPacket;
import cc.sighs.oelib.config.net.ConfigUpdateRequestPacket;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import cc.sighs.oelib.network.api.NetworkManager;
import cc.sighs.oelib.platform.Platform;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all server-side configurations and integrates with resource reload.
 * <p>
 * This manager is responsible for:
 * <ul>
 *     <li>Registering server configs backed by {@link ConfigUnit}</li>
 *     <li>Reloading server configs on data pack reload</li>
 *     <li>Encoding and applying remote updates for server-owned configs</li>
 * </ul>
 * The public facade {@link ConfigManager} delegates server-related operations here.
 * </p>
 */
public class ServerConfigManager implements ResourceManagerReloadListener {
    private static final Map<Identifier, ConfigUnit<?>> CONFIGS = new ConcurrentHashMap<>();
    private static final Map<Identifier, IConfigPermissionChecker> PERMISSIONS = new ConcurrentHashMap<>();
    private static final Map<Identifier, String> LAST_BROADCAST = new ConcurrentHashMap<>();
    private static final Map<Identifier, String> CLIENT_KNOWN_SERVER = new ConcurrentHashMap<>();

    static void registerUnit(ConfigUnit<?> unit, IConfigPermissionChecker permissionChecker) {
        var meta = unit.meta();
        if (meta.side() != ConfigSide.SERVER) {
            OELib.LOGGER.warn("Registering non-server config {} into ServerConfigManager (side={})", meta.id(), meta.side());
        }
        var existing = CONFIGS.put(unit.id(), unit);
        if (existing != null) {
            OELib.LOGGER.warn("Duplicate server config registration for {}, replacing previous unit", unit.id());
        }
        if (permissionChecker != null) {
            PERMISSIONS.put(unit.id(), permissionChecker);
        }
        unit.applyAutoMigrationOnRegister();
    }

    static Optional<ConfigUnit<?>> get(Identifier id) {
        return Optional.ofNullable(CONFIGS.get(id));
    }

    public static Map<Identifier, ConfigUnit<?>> all() {
        return Map.copyOf(CONFIGS);
    }

    public static Optional<IConfigPermissionChecker> getPermissionChecker(Identifier id) {
        return Optional.ofNullable(PERMISSIONS.get(id));
    }

    public static void recordBroadcast(Identifier id, String payload) {
        LAST_BROADCAST.put(id, payload);
        CLIENT_KNOWN_SERVER.put(id, payload);
    }

    public static Optional<String> getLastBroadcast(Identifier id) {
        return Optional.ofNullable(LAST_BROADCAST.get(id));
    }

    public static void recordClientKnownServer(Identifier id, String payload) {
        CLIENT_KNOWN_SERVER.put(id, payload);
    }

    public static Optional<String> getClientKnownServer(Identifier id) {
        return Optional.ofNullable(CLIENT_KNOWN_SERVER.get(id));
    }

    static void reloadAll() {
        for (ConfigUnit<?> unit : CONFIGS.values()) {
            unit.reload();
            if (Platform.isServer()) {
                var id = unit.id();
                var payloadOpt = encodeToString(id);
                payloadOpt.ifPresent(encoded -> {
                    recordBroadcast(unit.id(), encoded.payload());
                    if (Platform.getCurrentServer() != null && !Platform.getAllPlayers(Platform.getCurrentServer()).isEmpty()) {
                        NetworkManager.sendToAll(new ConfigSyncPacket(unit.id(), encoded.payload(), encoded.format()));
                    } else {
                        OELib.LOGGER.debug("Skipping config sync for {}: No players online or server starting.", unit.id());
                    }
                });
            } else {
                var id = unit.id();
                var format = unit.meta().format();
                @SuppressWarnings("unchecked")
                ConfigUnit<Object> cast = (ConfigUnit<Object>) unit;
                var encoded = ConfigSerializationUtil.encodeToString(cast.get(), format, cast.codec().codec(), cast.codec().fields());
                if (encoded.isPresent()) {
                    var known = getClientKnownServer(id);
                    if (known.isEmpty() || !known.get().equals(encoded.get())) {
                        if (Platform.getCurrentServer() != null && !Platform.getAllPlayers(Platform.getCurrentServer()).isEmpty()) {
                            NetworkManager.sendToServer(new ConfigUpdateRequestPacket(id, encoded.get(), format, true));
                        }
                    }
                }
            }
        }
    }

    static Optional<ConfigManager.EncodedPayload> encodeToString(Identifier id) {
        var unit = CONFIGS.get(id);
        if (unit == null) {
            return Optional.empty();
        }
        var format = unit.meta().format();
        @SuppressWarnings("unchecked")
        ConfigUnit<Object> cast = (ConfigUnit<Object>) unit;
        var encoded = ConfigSerializationUtil.encodeToString(cast.get(), format, cast.codec().codec(), cast.codec().fields());
        return encoded.map(s -> new ConfigManager.EncodedPayload(format, s));
    }

    static void applyRemoteUpdate(Identifier id, String payload, ConfigStorageFormat format) {
        var unit = CONFIGS.get(id);
        if (unit == null) {
            return;
        }
        var result = ConfigSerializationUtil.parse(payload, format, unit.codec().codec());
        if (result.error().isPresent()) {
            OELib.LOGGER.error("Failed to apply remote server config {}: {}", unit.id(), result.error().get().message());
            return;
        }
        result.result().ifPresent(v -> {
            @SuppressWarnings("unchecked")
            ConfigUnit<Object> cast = (ConfigUnit<Object>) unit;
            OELib.LOGGER.info("Applying remote update for server config {} with format {}", unit.id(), format);
            cast.setValue(v);
            ConfigEvents.onSync(cast, v, true);
            OELib.LOGGER.info("Applied remote update for server config {}", unit.id());
        });
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        reloadAll();
        OELib.LOGGER.info("Successfully reload {} server config(s).", CONFIGS.size());
    }
}
