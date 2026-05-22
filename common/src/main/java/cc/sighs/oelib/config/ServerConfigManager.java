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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for server-side configuration units.
 *
 * <p>This manager stores all configurations whose {@link ConfigSide} is
 * {@link ConfigSide#SERVER}. In addition to the basic register/lookup/reload
 * operations, it handles:
 * <ul>
 *   <li>Encoding config values for network synchronization</li>
 *   <li>Applying remote updates received from clients (with permission checks)</li>
 *   <li>Broadcasting config changes to connected players via
 *       {@link ConfigSyncPacket}</li>
 *   <li>Tracking the last broadcast payload to avoid redundant syncs</li>
 * </ul>
 *
 * <p>The public-facing {@link ConfigManager} delegates server-related
 * operations to this class. Direct use is rarely needed outside the
 * framework internals.
 */
public class ServerConfigManager implements ResourceManagerReloadListener {
    private static final Map<ResourceLocation, ConfigUnit<?>> CONFIGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, IConfigPermissionChecker> PERMISSIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> LAST_BROADCAST = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> CLIENT_KNOWN_SERVER = new ConcurrentHashMap<>();

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

    static Optional<ConfigUnit<?>> get(ResourceLocation id) {
        return Optional.ofNullable(CONFIGS.get(id));
    }

    /**
     * Returns an unmodifiable snapshot of all registered server configurations.
     *
     * @return a map of configuration ids to units
     */
    public static Map<ResourceLocation, ConfigUnit<?>> all() {
        return Map.copyOf(CONFIGS);
    }

    /**
     * Returns the permission checker registered for the given configuration,
     * if any.
     *
     * @param id the configuration id
     * @return the permission checker, or {@link Optional#empty()}
     */
    public static Optional<IConfigPermissionChecker> getPermissionChecker(ResourceLocation id) {
        return Optional.ofNullable(PERMISSIONS.get(id));
    }

    /**
     * Records a broadcast payload for the given configuration so that
     * redundant syncs can be suppressed.
     *
     * @param id      the configuration id
     * @param payload the encoded payload
     */
    public static void recordBroadcast(ResourceLocation id, String payload) {
        LAST_BROADCAST.put(id, payload);
        CLIENT_KNOWN_SERVER.put(id, payload);
    }

    /**
     * Returns the last broadcast payload for the given configuration.
     *
     * @param id the configuration id
     * @return the last broadcast payload, or {@link Optional#empty()}
     */
    public static Optional<String> getLastBroadcast(ResourceLocation id) {
        return Optional.ofNullable(LAST_BROADCAST.get(id));
    }

    /**
     * Records a payload that the client is known to have for the given
     * configuration.
     *
     * @param id      the configuration id
     * @param payload the encoded payload
     */
    public static void recordClientKnownServer(ResourceLocation id, String payload) {
        CLIENT_KNOWN_SERVER.put(id, payload);
    }

    /**
     * Returns the payload that the client is known to have for the given
     * configuration.
     *
     * @param id the configuration id
     * @return the known payload, or {@link Optional#empty()}
     */
    public static Optional<String> getClientKnownServer(ResourceLocation id) {
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

    static Optional<ConfigManager.EncodedPayload> encodeToString(ResourceLocation id) {
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

    static void applyRemoteUpdate(ResourceLocation id, String payload, ConfigStorageFormat format) {
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
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        reloadAll();
        OELib.LOGGER.info("Successfully reload {} server config(s).", CONFIGS.size());
    }
}
