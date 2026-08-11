package cc.sighs.oelib.config.util;

import cc.sighs.oelib.config.*;
import cc.sighs.oelib.config.datafix.ConfigFixRegistry;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.net.ConfigSyncPacket;
import cc.sighs.oelib.config.validation.ConfigValidationException;
import cc.sighs.oelib.network.api.NetworkManager;
import cc.sighs.oelib.platform.Platform;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.util.OptionalOps;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

        Try.of(() -> {
            ConfigLifecycle.reload(unit);
            var currentEncodedOpt = OptionalOps.toMaybe(ConfigManager.encodeToString(unit.id()));
            var lastBroadcastOpt = ServerConfigManager.getLastBroadcast(unit.id());
            if (currentEncodedOpt.isDefined() && lastBroadcastOpt.isDefined() && !currentEncodedOpt.get().payload().equals(lastBroadcastOpt.get())) {
                OELibConfig.LOGGER.info("Server-side change detected for config {}, ignoring client update and broadcasting server state", unit.id());
                NetworkManager.sendToAll(new ConfigSyncPacket(unit.id(), currentEncodedOpt.get().payload(), currentEncodedOpt.get().format()));
                ServerConfigManager.recordBroadcast(unit.id(), currentEncodedOpt.get().payload());
                return Unit.INSTANCE;
            }
            var result = ConfigSerializationUtil.parse(payload, format, unit.codec().codec());
            var parseError = OptionalOps.toMaybe(result.error());
            if (parseError.isDefined()) {
                OELibConfig.LOGGER.error("Failed to parse update for config {}: {}", unit.id(), parseError.get().message());
                return Unit.INSTANCE;
            }
            OptionalOps.toMaybe(result.result()).ifPresent(v -> {
                OELibConfig.LOGGER.info("Server applying update for config {} requested by {} with format {} (save={})",
                        unit.id(), player.getGameProfile().getName(), format, save);
                ConfigLifecycle.replace(unit, v, save);
            });
            return Unit.INSTANCE;
        }).peekFailure(error -> OELibConfig.LOGGER.error(
                "Exception while applying server config update {}", unit.id(), error));
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
        Try.of(() -> {
            if (!Files.exists(path)) {
                var meta = unit.meta();
                int version = ConfigFixRegistry.currentVersion(
                        meta.id(), unit.codec().fields());
                var content = ConfigSerializationUtil.encodeToStringWithVersion(
                        unit.getDefaultValue(), version, meta.format(), unit.codec().codec(), unit.codec().fields()
                );
                boolean success = content.isDefined();
                if (success) {
                    var parent = path.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    writeAtomically(path, content.get());
                }
                if (success) {
                    OELibConfig.LOGGER.info("Initialized config {} at {}", meta.id(), path);
                } else {
                    OELibConfig.LOGGER.error("Failed to initialize config {} at {}", meta.id(), path);
                }
            }
            return Unit.INSTANCE;
        }).peekFailure(error -> OELibConfig.LOGGER.error(
                "Exception during config initialization {}", unit.meta().id(), error));
    }

    /**
     * Applies configured migrations and storage-format changes during registration.
     *
     * <p>A migration, decoding, validation, encoding, or write failure is logged and leaves the
     * source file unchanged.
     *
     * @param unit the configuration unit
     * @param <T>  the configuration value type
     */
    public static <T> void applyAutoMigrationOnRegister(ConfigUnit<T> unit) {
        var meta = unit.meta();
        Try.of(() -> {
            var existing = findExistingPathAnyFormat(meta);
            var target = resolveSavePath(meta);
            if (existing == null && !Files.exists(target)) {
                initializeIfMissing(unit);
                return Unit.INSTANCE;
            }
            var source = existing != null ? existing : target;
            String raw = Files.readString(source, StandardCharsets.UTF_8);
            var formatSource = detectFormatFromPath(source, meta.format());
            var dyn = ConfigSerializationUtil.parseToDynamic(raw, formatSource);
            int inputVersion = dyn.get("__cfg_version").asInt(0);
            int currentVersion = ConfigFixRegistry.currentVersion(
                    meta.id(), unit.codec().fields());
            var fixed = ConfigFixRegistry.apply(
                    meta.id(), dyn, inputVersion, unit.codec().fields());
            if (fixed.left().isPresent()) {
                var error = fixed.left().orElseThrow();
                throw new IllegalStateException(
                        "Config migration failed [" + error.code() + "]: " + error.message(),
                        error.cause());
            }
            var migrated = fixed.right().orElseThrow();
            var result = unit.codec().codec().parse(migrated);
            var decodeError = OptionalOps.toMaybe(result.error());
            if (decodeError.isDefined()) {
                throw new IllegalStateException(
                        "Config decode failed for " + meta.id() + ": "
                                + decodeError.get().message());
            }
            var value = OptionalOps.toMaybe(result.result()).fold(
                    () -> { throw new IllegalStateException(
                            "Config decode returned no value for " + meta.id()); },
                    decodedValue -> decodedValue);
            unit.validate(value).ifPresent(report -> {
                throw new ConfigValidationException(report);
            });
            var encoded = ConfigSerializationUtil.encodeToStringWithVersion(
                    value, currentVersion, meta.format(), unit.codec().codec(), unit.codec().fields()
            );
            if (encoded.isEmpty()) {
                return Unit.INSTANCE;
            }
            String newContent = encoded.get();
            boolean changed = !normalize(raw).equals(normalize(newContent)) ||
                    (existing != null && !existing.equals(target));
            if (changed) {
                var parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                writeAtomically(target, newContent);
                if (existing != null && !existing.equals(target) && Files.exists(existing)) {
                    Files.delete(existing);
                }
                OELibConfig.LOGGER.info("Migrated and rewrote config {} to {}", meta.id(), target);
                ConfigLifecycle.reload(unit);
            } else {
                ConfigLifecycle.reload(unit);
            }
            return Unit.INSTANCE;
        }).peekFailure(error -> OELibConfig.LOGGER.error(
                "Exception during auto migration for {}", meta.id(), error));
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

    private static void writeAtomically(Path path, String content) throws Exception {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
