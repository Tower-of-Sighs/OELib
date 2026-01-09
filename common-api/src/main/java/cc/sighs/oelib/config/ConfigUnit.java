package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.api.ConfigEvents;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import cc.sighs.oelib.platform.Platform;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime unit responsible for config IO, caching and event dispatch.
 * <p>
 * Encapsulates a {@link ConfigCodec} and provides lazy loading, hot reloading,
 * and saving to either JSON or TOML backends based on {@link cc.sighs.oelib.config.model.ConfigMeta}.
 * </p>
 */
public class ConfigUnit<T> {
    private final ConfigCodec<T> configCodec;
    private final T defaultValue;
    private final AtomicBoolean loaded = new AtomicBoolean();
    private volatile T currentValue;

    private ConfigUnit(ConfigCodec<T> configCodec, T defaultValue) {
        this.configCodec = configCodec;
        this.defaultValue = defaultValue;
    }

    public static <T> ConfigUnit<T> of(ConfigCodec<T> codec, T defaultValue) {
        Objects.requireNonNull(codec);
        Objects.requireNonNull(defaultValue);
        return new ConfigUnit<>(codec, defaultValue);
    }

    public T get() {
        if (loaded.compareAndSet(false, true)) {
            try {
                this.currentValue = load();
            } catch (Exception e) {
                OELib.LOGGER.error("Failed to load config {}", configCodec.meta().id(), e);
                this.currentValue = defaultValue;
            }
        }
        return currentValue;
    }

    public void reload() {
        try {
            this.currentValue = load();
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to reload config {}", configCodec.meta().id(), e);
        }
    }

    public void initializeIfMissing() {
        var path = resolveSavePath();
        try {
            if (!Files.exists(path)) {
                var meta = configCodec.meta();
                boolean success = ConfigSerializationUtil.saveToFile(path, defaultValue, meta.format(), configCodec.codec(), configCodec.fields());
                if (success) {
                    OELib.LOGGER.info("Initialized config {} at {}", meta.id(), path);
                } else {
                    OELib.LOGGER.error("Failed to initialize config {} at {}", meta.id(), path);
                }
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Exception during config initialization {}", configCodec.meta().id(), e);
        }
    }

    private T load() {
        var meta = configCodec.meta();
        var path = resolveLoadPath();
        var loaded = ConfigSerializationUtil.loadFromFile(path, meta.format(), configCodec.codec(), defaultValue);
        T value = loaded.orElse(defaultValue);
        OELib.LOGGER.info("Loaded config {} from {}", meta.id(), path);
        ConfigEvents.onLoad(this, value);
        return value;
    }

    public void save() {
        T value = currentValue != null ? currentValue : defaultValue;
        var meta = configCodec.meta();
        var path = resolveSavePath();
        if (meta.side() == ConfigSide.SERVER && Platform.isClient()) {
            if (ConfigManager.isUpdatingFromServer()) {
                OELib.LOGGER.debug("Skipping client-side save for server config {} during server sync", meta.id());
            }
            return;
        }
        try {
            ConfigEvents.beforeSave(this, value);
            boolean success = ConfigSerializationUtil.saveToFile(path, value, meta.format(), configCodec.codec(), configCodec.fields());
            if (!success) {
                throw new IllegalStateException("Failed to save config: " + meta.id());
            }
            OELib.LOGGER.info("Saved config {} to {}", meta.id(), path);
            ConfigEvents.afterSave(this, value);
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to save config {}", meta.id(), e);
        }
    }


    public ConfigMeta meta() {
        return configCodec.meta();
    }

    public ResourceLocation id() {
        return configCodec.meta().id();
    }

    public ConfigCodec<T> codec() {
        return configCodec;
    }

    public void setValue(T value) {
        var old = this.currentValue;
        this.currentValue = value;
        if (Platform.isClient() && configCodec.meta().side() == ConfigSide.SERVER && ConfigManager.isUpdatingFromServer()) {
            return;
        }
        OELib.LOGGER.info("Config {} changed", configCodec.meta().id());
        ConfigEvents.onChanged(this, old, value);
    }

    private Path resolveBaseDirectory() {
        var meta = configCodec.meta();
        var base = Platform.getConfigPath();
        if (meta.directory() != null && !meta.directory().isEmpty()) {
            base = base.resolve(meta.directory());
        }
        return base;
    }

    private Path resolveLoadPath() {
        var meta = configCodec.meta();
        var base = resolveBaseDirectory();
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

    private Path resolveSavePath() {
        var meta = configCodec.meta();
        var base = resolveBaseDirectory();
        var name = meta.fileName();
        if (name.contains(".")) {
            return base.resolve(name);
        }
        var ext = meta.format().name().toLowerCase();
        return base.resolve(name + "." + ext);
    }
}
