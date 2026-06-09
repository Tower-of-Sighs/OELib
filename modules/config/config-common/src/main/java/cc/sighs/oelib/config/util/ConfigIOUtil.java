package cc.sighs.oelib.config.util;

import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.OELibConfig;
import cc.sighs.oelib.config.ServerConfigManager;
import cc.sighs.oelib.config.datafix.ConfigFixRegistry;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.net.ConfigSyncPacket;
import cc.sighs.oelib.network.api.NetworkManager;
import cc.sighs.oelib.platform.Platform;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File I/O helpers for loading, saving, migrating, and applying remote
 * updates to configuration files.
 */
public final class ConfigIOUtil {

    private ConfigIOUtil() {
    }

    /**
     * Applies a client-originated update to a server configuration, after
     * verifying permissions and checking for concurrent server-side changes.
     *
     * @param unit    the configuration unit
     * @param player  the player requesting the update
     * @param payload the encoded configuration content
     * @param format  the serialization format
     * @param save    {@code true} to persist the update to disk
     * @param <T>     the configuration value type
     */
    public static <T> void applyUpdate(ConfigUnit<T> unit, ServerPlayer player, String payload, ConfigStorageFormat format, boolean save) {
        if (unit.meta().side() != ConfigSide.SERVER) {
            OELibConfig.LOGGER.warn("Rejected update for non-server config {}", unit.id());
            return;
        }
        var checkerOpt = ServerConfigManager.getPermissionChecker(unit.id());
        if (checkerOpt.isEmpty()) {
            OELibConfig.LOGGER.warn("No permission checker for server config {}; rejecting client update", unit.id());
            return;
        }
        var checker = checkerOpt.get();
        if (!checker.canUpdate(player)) {
            OELibConfig.LOGGER.warn("Player {} not permitted to update config {}", player.getGameProfile().getName(), unit.id());
            return;
        }

        try {
            unit.reload();
            var currentEncodedOpt = ConfigManager.encodeToString(unit.id());
            var lastBroadcastOpt = ServerConfigManager.getLastBroadcast(unit.id());
            if (currentEncodedOpt.isPresent() && lastBroadcastOpt.isPresent() && !currentEncodedOpt.get().payload().equals(lastBroadcastOpt.get())) {
                OELibConfig.LOGGER.info("Server-side change detected for config {}, ignoring client update and broadcasting server state", unit.id());
                NetworkManager.sendToAll(new ConfigSyncPacket(unit.id(), currentEncodedOpt.get().payload(), currentEncodedOpt.get().format()));
                ServerConfigManager.recordBroadcast(unit.id(), currentEncodedOpt.get().payload());
                return;
            }
            var result = ConfigSerializationUtil.parse(payload, format, unit.codec().codec());
            if (result.error().isPresent()) {
                OELibConfig.LOGGER.error("Failed to parse update for config {}: {}", unit.id(), result.error().get().message());
                return;
            }
            result.result().ifPresent(v -> {
                OELibConfig.LOGGER.info("Server applying update for config {} requested by {} with format {} (save={})",
                        unit.id(), player.getGameProfile().getName(), format, save);
                unit.setValue(v);
                if (save) {
                    unit.save();
                }
                ConfigManager.encodeToString(unit.id()).ifPresent(encoded -> {
                    OELibConfig.LOGGER.info("Broadcasting config {} to clients", unit.id());
                    NetworkManager.sendToAll(new ConfigSyncPacket(unit.id(), encoded.payload(), encoded.format()));
                    ServerConfigManager.recordBroadcast(unit.id(), encoded.payload());
                });
            });
        } catch (Exception e) {
            OELibConfig.LOGGER.error("Exception while applying server config update {}", unit.id(), e);
        }
    }

    /**
     * Creates a configuration file with the default value if it does not
     * already exist on disk.
     *
     * @param unit the configuration unit
     * @param <T>  the configuration value type
     */
    public static <T> void initializeIfMissing(ConfigUnit<T> unit) {
        var path = resolveSavePath(unit.meta());
        try {
            if (!Files.exists(path)) {
                var meta = unit.meta();
                var chainOpt = ConfigFixRegistry.get(meta.id());
                int version = chainOpt.map(ConfigFixRegistry.Chain::currentVersion).orElse(0);
                var content = ConfigSerializationUtil.encodeToStringWithVersion(
                        unit.getDefaultValue(), version, meta.format(), unit.codec().codec(), unit.codec().fields()
                );
                boolean success = content.isPresent();
                if (success) {
                    var parent = path.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.writeString(path, content.get(), StandardCharsets.UTF_8);
                }
                if (success) {
                    OELibConfig.LOGGER.info("Initialized config {} at {}", meta.id(), path);
                } else {
                    OELibConfig.LOGGER.error("Failed to initialize config {} at {}", meta.id(), path);
                }
            }
        } catch (Exception e) {
            OELibConfig.LOGGER.error("Exception during config initialization {}", unit.meta().id(), e);
        }
    }

    /**
     * Runs automatic migration on a configuration file when a unit is
     * registered, applying datafix chains and migrating format if needed.
     *
     * @param unit the configuration unit
     * @param <T>  the configuration value type
     */
    public static <T> void applyAutoMigrationOnRegister(ConfigUnit<T> unit) {
        var meta = unit.meta();
        try {
            var existing = findExistingPathAnyFormat(meta);
            var target = resolveSavePath(meta);
            if (existing == null && !Files.exists(target)) {
                initializeIfMissing(unit);
                return;
            }
            var source = existing != null ? existing : target;
            String raw = Files.readString(source, StandardCharsets.UTF_8);
            var formatSource = detectFormatFromPath(source, meta.format());
            var dyn = ConfigSerializationUtil.parseToDynamic(raw, formatSource);
            int inputVersion = dyn.get("__cfg_version").asInt(0);
            var chainOpt = ConfigFixRegistry.get(meta.id());
            var migrated = dyn;
            int currentVersion = 0;
            if (chainOpt.isPresent()) {
                var chain = chainOpt.get();
                currentVersion = chain.currentVersion();
                migrated = chain.apply(dyn, inputVersion);
            }
            var result = unit.codec().codec().parse(migrated);
            var value = result.result().orElse(unit.getDefaultValue());
            var encoded = ConfigSerializationUtil.encodeToStringWithVersion(
                    value, currentVersion, meta.format(), unit.codec().codec(), unit.codec().fields()
            );
            if (encoded.isEmpty()) {
                return;
            }
            String newContent = encoded.get();
            boolean changed = !normalize(raw).equals(normalize(newContent)) ||
                    (existing != null && !existing.equals(target));
            if (changed) {
                var parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(target, newContent, StandardCharsets.UTF_8);
                if (existing != null && !existing.equals(target) && Files.exists(existing)) {
                    try {
                        Files.delete(existing);
                    } catch (Exception ignored) {
                    }
                }
                OELibConfig.LOGGER.info("Migrated and rewrote config {} to {}", meta.id(), target);
                unit.reload();
            } else {
                unit.reload();
            }
        } catch (Exception e) {
            OELibConfig.LOGGER.error("Exception during auto migration for {}", meta.id(), e);
        }
    }

    /**
     * Resolves the base directory for a configuration, considering any
     * custom sub-directory in its metadata.
     *
     * @param meta the configuration metadata
     * @return the base directory path
     */
    public static Path resolveBaseDirectory(ConfigMeta meta) {
        var base = Platform.getConfigPath();
        if (meta.directory() != null && !meta.directory().isEmpty()) {
            base = base.resolve(meta.directory());
        }
        return base;
    }

    /**
     * Resolves the load path, trying extension-based lookup and falling
     * back to the configured extension.
     *
     * @param meta the configuration metadata
     * @return the resolved load path
     */
    public static Path resolveLoadPath(ConfigMeta meta) {
        var base = resolveBaseDirectory(meta);
        var name = meta.fileName();
        if (name.contains(".")) {
            return base.resolve(name);
        }
        var ext = meta.format().name().toLowerCase();
        var extPath = base.resolve(name + "." + ext);
        if (Files.exists(extPath)) {
            return extPath;
        }
        var directPath = base.resolve(name);
        if (Files.exists(directPath)) {
            return directPath;
        }
        return extPath;
    }

    /**
     * Resolves the save path for a configuration, always using the
     * configured extension.
     *
     * @param meta the configuration metadata
     * @return the resolved save path
     */
    public static Path resolveSavePath(ConfigMeta meta) {
        var base = resolveBaseDirectory(meta);
        var name = meta.fileName();
        if (name.contains(".")) {
            return base.resolve(name);
        }
        var ext = meta.format().name().toLowerCase();
        return base.resolve(name + "." + ext);
    }

    /**
     * Searches for an existing configuration file in any supported format.
     *
     * @param meta the configuration metadata
     * @return the path if found, or {@code null}
     */
    public static Path findExistingPathAnyFormat(ConfigMeta meta) {
        var base = resolveBaseDirectory(meta);
        var name = meta.fileName();
        if (name.contains(".")) {
            var p = base.resolve(name);
            return Files.exists(p) ? p : null;
        }
        for (var fmt : ConfigStorageFormat.values()) {
            var ext = fmt.name().toLowerCase();
            var p = base.resolve(name + "." + ext);
            if (Files.exists(p)) {
                return p;
            }
        }
        var direct = base.resolve(name);
        return Files.exists(direct) ? direct : null;
    }

    /**
     * Detects the storage format from a file path by examining its extension.
     *
     * @param path     the file path
     * @param fallback the format to return if detection fails
     * @return the detected format
     */
    public static ConfigStorageFormat detectFormatFromPath(Path path, ConfigStorageFormat fallback) {
        String fn = path.getFileName().toString().toLowerCase();
        if (fn.endsWith(".json")) return ConfigStorageFormat.JSON;
        if (fn.endsWith(".json5")) return ConfigStorageFormat.JSON5;
        if (fn.endsWith(".toml")) return ConfigStorageFormat.TOML;
        return fallback;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").trim();
    }
}
