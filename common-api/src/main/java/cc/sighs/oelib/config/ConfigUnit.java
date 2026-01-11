package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.api.ConfigEvents;
import cc.sighs.oelib.config.datafix.ConfigFixRegistry;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.util.ConfigIOUtil;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import cc.sighs.oelib.platform.Platform;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    public void applyAutoMigrationOnRegister() {
        ConfigIOUtil.applyAutoMigrationOnRegister(this);
    }

    private T load() {
        var meta = configCodec.meta();
        var path = ConfigIOUtil.resolveLoadPath(meta);
        var loaded = ConfigSerializationUtil.loadFromFile(path, meta.format(), configCodec.codec(), defaultValue);
        T value = loaded.orElse(defaultValue);
        OELib.LOGGER.info("Loaded config {} from {}", meta.id(), path);
        ConfigEvents.onLoad(this, value);
        return value;
    }

    public void save() {
        var value = currentValue != null ? currentValue : defaultValue;
        var meta = configCodec.meta();
        var path = ConfigIOUtil.resolveSavePath(meta);
        try {
            ConfigEvents.beforeSave(this, value);
            int version = ConfigFixRegistry.get(meta.id()).map(ConfigFixRegistry.Chain::currentVersion).orElse(0);
            var content = ConfigSerializationUtil.encodeToStringWithVersion(value, version, meta.format(), configCodec.codec(), configCodec.fields());
            if (content.isEmpty()) {
                throw new IllegalStateException("Failed to save config: " + meta.id());
            }
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content.get(), StandardCharsets.UTF_8);
            OELib.LOGGER.info("Saved config {} to {}", meta.id(), path);
            ConfigEvents.afterSave(this, value);
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to save config {}", meta.id(), e);
        }
    }

    public T getDefaultValue() {
        return defaultValue;
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
}
