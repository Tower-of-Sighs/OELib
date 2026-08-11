package cc.sighs.oelib.config;

import cc.sighs.oelib.config.api.ConfigEvents;
import cc.sighs.oelib.config.api.IConfigPermissionChecker;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.net.ConfigSyncPacket;
import cc.sighs.oelib.config.net.ConfigUpdateRequestPacket;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import cc.sighs.oelib.config.validation.ConfigValidationException;
import cc.sighs.oelib.network.api.NetworkManager;
import cc.sighs.oelib.platform.Platform;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.util.OptionalOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers, reloads, and synchronizes server-side configuration units.
 *
 * <p>A resource-manager reload reloads registered values and synchronizes changed payloads.
 * Duplicate identifiers replace the previously registered unit.
 */
@ApiStatus.Internal
public class ServerConfigManager implements ResourceManagerReloadListener {
    private static final Map<ResourceLocation, ConfigUnit<?>> CONFIGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, IConfigPermissionChecker> PERMISSIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> LAST_BROADCAST = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> CLIENT_KNOWN_SERVER = new ConcurrentHashMap<>();

    static void registerUnit(ConfigUnit<?> unit, IConfigPermissionChecker permissionChecker) {
        var meta = unit.meta();
        if (meta.side() != ConfigSide.SERVER) {
            OELibConfig.LOGGER.warn("Registering non-server config {} into ServerConfigManager (side={})", meta.id(), meta.side());
        }
        var existing = CONFIGS.put(unit.id(), unit);
        if (existing != null) {
            OELibConfig.LOGGER.warn("Duplicate server config registration for {}, replacing previous unit", unit.id());
        }
        if (permissionChecker != null) {
            PERMISSIONS.put(unit.id(), permissionChecker);
        }
        unit.applyAutoMigrationOnRegister();
    }

    static Maybe<ConfigUnit<?>> get(ResourceLocation id) {
        return Maybe.ofNullable(CONFIGS.get(id));
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
     * @return the permission checker, or an empty value
     */
    public static Maybe<IConfigPermissionChecker> getPermissionChecker(ResourceLocation id) {
        return Maybe.ofNullable(PERMISSIONS.get(id));
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
     * @return the last broadcast payload, or an empty value
     */
    public static Maybe<String> getLastBroadcast(ResourceLocation id) {
        return Maybe.ofNullable(LAST_BROADCAST.get(id));
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
     * @return the known payload, or an empty value
     */
    public static Maybe<String> getClientKnownServer(ResourceLocation id) {
        return Maybe.ofNullable(CLIENT_KNOWN_SERVER.get(id));
    }

    static <T> void synchronizeAccepted(ConfigUnit<T> unit, T value) {
        if (CONFIGS.get(unit.id()) != unit) {
            return;
        }
        var format = unit.meta().format();
        var encoded = ConfigSerializationUtil.encodeToString(
                value, format, unit.codec().codec(), unit.codec().fields());
        if (encoded.isEmpty()) {
            OELibConfig.LOGGER.error(
                    "Failed to encode accepted server config {} for synchronization", unit.id());
            return;
        }
        recordBroadcast(unit.id(), encoded.get());
        if (Platform.getCurrentServer() != null
                && !Platform.getAllPlayers(Platform.getCurrentServer()).isEmpty()) {
            NetworkManager.sendToAll(new ConfigSyncPacket(unit.id(), encoded.get(), format));
        }
        ConfigEvents.onSync(unit, value, false);
    }

    static void reloadAll() {
        for (ConfigUnit<?> unit : CONFIGS.values()) {
            ConfigLifecycle.reload(unit);
            if (Platform.isServer()) {
                var id = unit.id();
                var payloadOpt = encodeToString(id);
                payloadOpt.ifPresent(encoded -> {
                    recordBroadcast(unit.id(), encoded.payload());
                    if (Platform.getCurrentServer() != null && !Platform.getAllPlayers(Platform.getCurrentServer()).isEmpty()) {
                        NetworkManager.sendToAll(new ConfigSyncPacket(unit.id(), encoded.payload(), encoded.format()));
                    } else {
                        OELibConfig.LOGGER.debug("Skipping config sync for {}: No players online or server starting.", unit.id());
                    }
                });
            } else {
                var id = unit.id();
                var format = unit.meta().format();
                @SuppressWarnings("unchecked")
                ConfigUnit<Object> cast = (ConfigUnit<Object>) unit;
                var encoded = ConfigSerializationUtil.encodeToString(cast.get(), format, cast.codec().codec(), cast.codec().fields());
                if (encoded.isDefined()) {
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

    static Maybe<ConfigManager.EncodedPayload> encodeToString(ResourceLocation id) {
        var unit = CONFIGS.get(id);
        if (unit == null) {
            return Maybe.none();
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
        var parseError = OptionalOps.toMaybe(result.error());
        if (parseError.isDefined()) {
            OELibConfig.LOGGER.error("Failed to apply remote server config {}: {}", unit.id(), parseError.get().message());
            return;
        }
        OptionalOps.toMaybe(result.result()).ifPresent(v -> {
            @SuppressWarnings("unchecked")
            ConfigUnit<Object> cast = (ConfigUnit<Object>) unit;
            Try.of(() -> {
                OELibConfig.LOGGER.info(
                        "Applying remote update for server config {} with format {}",
                        unit.id(), format);
                Object accepted = ConfigLifecycle.replace(cast, v, false);
                ConfigEvents.onSync(cast, accepted, true);
                OELibConfig.LOGGER.info("Applied remote update for server config {}", unit.id());
                return accepted;
            }).peekFailure(error -> {
                if (error instanceof ConfigValidationException validation) {
                    OELibConfig.LOGGER.warn(
                            "Rejected remote update for server config {}: {}",
                            unit.id(), validation.report().summary());
                } else {
                    OELibConfig.LOGGER.error(
                            "Failed to apply remote update for server config {}", unit.id(), error);
                }
            });
        });
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        reloadAll();
        OELibConfig.LOGGER.info("Successfully reload {} server config(s).", CONFIGS.size());
    }
}
