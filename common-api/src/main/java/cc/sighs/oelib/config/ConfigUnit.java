package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.api.ConfigEvents;
import cc.sighs.oelib.config.model.ConfigMeta;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import cc.sighs.oelib.platform.Platform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.marhali.json5.Json5;
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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Json5 JSON5 = Json5.builder(builder -> builder.parseComments().build());
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

    private T load() {
        var meta = configCodec.meta();
        var path = resolveLoadPath();
        var loaded = ConfigSerializationUtil.loadFromFile(path, meta.format(), configCodec.codec(), defaultValue);
        T value = loaded.orElse(defaultValue);
        ConfigEvents.onLoad(this, value);
        return value;
    }

    public void save() {
        T value = currentValue != null ? currentValue : defaultValue;
        var meta = configCodec.meta();
        var path = resolveSavePath();
        try {
            ConfigEvents.beforeSave(this, value);
            boolean success = ConfigSerializationUtil.saveToFile(path, value, meta.format(), configCodec.codec(), configCodec.fields());
            if (!success) {
                throw new IllegalStateException("Failed to save config: " + meta.id());
            }
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
        T old = this.currentValue;
        this.currentValue = value;
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
