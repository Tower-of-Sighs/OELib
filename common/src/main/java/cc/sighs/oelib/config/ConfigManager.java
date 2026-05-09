package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.api.IConfigPermissionChecker;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Central registry and coordination point for configuration units.
 *
 * <p>{@code ConfigManager} provides the public API for registering,
 * looking up, reloading, and applying remote updates to configurations.
 * It delegates client-side operations to {@link ClientConfigManager} and
 * server-side operations to {@link ServerConfigManager}, routing each
 * {@link ConfigUnit} based on its declared {@link ConfigSide}.
 *
 * <p>Server-synchronization state is tracked per thread via a
 * thread-local flag exposed through {@link #isUpdatingFromServer()}.
 * The {@link #runWithServerUpdate(Runnable)} and
 * {@link #callWithServerUpdate(Supplier)} methods bracket operations
 * that originate from a server push so that the unit can suppress
 * redundant change events.
 */
public final class ConfigManager {

    private static final ThreadLocal<Boolean> UPDATING_FROM_SERVER = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ConfigManager() {
    }

    /**
     * Registers a client-side configuration unit.
     *
     * @param unit the configuration unit to register
     * @throws NullPointerException if {@code unit} is {@code null}
     */
    public static void registerClient(ConfigUnit<?> unit) {
        registerUnit(unit);
    }

    /**
     * Registers a server-side configuration unit with an optional permission checker.
     *
     * @param unit              the configuration unit to register
     * @param permissionChecker the permission checker, or {@code null} to reject all client updates
     * @throws NullPointerException if {@code unit} is {@code null}
     */
    public static void registerServer(ConfigUnit<?> unit, IConfigPermissionChecker permissionChecker) {
        registerUnitServer(unit, permissionChecker);
    }

    /**
     * Creates a {@link ConfigUnit} from a codec and default value,
     * registers it, and returns the unit.
     *
     * @param codec        the codec describing the configuration
     * @param defaultValue the default value used when no file exists
     * @param <T>          the type of the configuration value
     * @return the registered configuration unit
     * @throws NullPointerException if {@code codec} or {@code defaultValue} is {@code null}
     */
    public static <T> ConfigUnit<T> register(ConfigCodec<T> codec, T defaultValue) {
        var unit = ConfigUnit.of(codec, defaultValue);
        registerUnit(unit);
        return unit;
    }

    /**
     * Registers a configuration unit, routing it to the appropriate manager
     * based on its declared side.
     *
     * <p>If the unit's metadata is incomplete (missing filename, format, or side),
     * an error is logged but registration still proceeds.
     *
     * @param unit the configuration unit to register
     * @throws NullPointerException if {@code unit} is {@code null}
     */
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

    /**
     * Registers a configuration unit directly with the server manager.
     *
     * @param unit              the configuration unit
     * @param permissionChecker the permission checker, or {@code null}
     */
    public static void registerUnitServer(ConfigUnit<?> unit, IConfigPermissionChecker permissionChecker) {
        ServerConfigManager.registerUnit(unit, permissionChecker);
    }

    /**
     * Looks up a configuration unit by its identifier, searching the server
     * registry first, then the client registry.
     *
     * @param id the configuration identifier
     * @return an {@link Optional} containing the unit, or {@link Optional#empty()} if not found
     */
    public static Optional<ConfigUnit<?>> get(Identifier id) {
        var server = ServerConfigManager.get(id);
        if (server.isPresent()) {
            return server;
        }
        return ClientConfigManager.get(id);
    }

    /**
     * Reloads the configuration identified by {@code id} from disk.
     *
     * @param id the configuration identifier
     */
    public static void reload(Identifier id) {
        get(id).ifPresent(ConfigUnit::reload);
    }

    /**
     * Reloads all registered configurations from disk.
     */
    public static void reloadAll() {
        reloadAllServer();
        reloadAllClient();
    }

    /**
     * Reloads all server-side configurations from disk.
     */
    public static void reloadAllServer() {
        ServerConfigManager.reloadAll();
    }

    /**
     * Reloads all client-side configurations from disk.
     */
    public static void reloadAllClient() {
        ClientConfigManager.reloadAll();
    }

    /**
     * Applies a remote payload to the server configuration identified by {@code id}
     * and fires synchronization events.
     *
     * @param id      the configuration identifier
     * @param payload the encoded configuration string
     * @param format  the serialization format of the payload
     */
    public static void applyRemoteUpdate(Identifier id, String payload, ConfigStorageFormat format) {
        ServerConfigManager.applyRemoteUpdate(id, payload, format);
    }

    /**
     * Returns {@code true} if the current thread is processing a server-pushed
     * configuration update.
     *
     * @return {@code true} if a server update is in progress
     */
    public static boolean isUpdatingFromServer() {
        return UPDATING_FROM_SERVER.get();
    }

    /**
     * Executes the given action with the server-update flag set.
     *
     * @param runnable the action to execute
     */
    public static void runWithServerUpdate(Runnable runnable) {
        boolean previous = UPDATING_FROM_SERVER.get();
        UPDATING_FROM_SERVER.set(Boolean.TRUE);
        try {
            runnable.run();
        } finally {
            UPDATING_FROM_SERVER.set(previous);
        }
    }

    /**
     * Executes the given supplier with the server-update flag set and returns
     * its result.
     *
     * @param supplier the supplier to execute
     * @param <T>      the type returned by the supplier
     * @return the value returned by the supplier
     */
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
     * Encodes the current value of the configuration identified by {@code id}
     * into a textual payload using its configured storage format.
     *
     * @param id the configuration identifier
     * @return an {@link Optional} containing the encoded payload and format,
     *         or {@link Optional#empty()} if the configuration is not found
     */
    public static Optional<EncodedPayload> encodeToString(Identifier id) {
        return ServerConfigManager.encodeToString(id);
    }

    /**
     * A format-tagged encoded configuration payload.
     *
     * @param format  the storage format
     * @param payload the encoded content
     */
    public record EncodedPayload(ConfigStorageFormat format, String payload) {
    }
}
