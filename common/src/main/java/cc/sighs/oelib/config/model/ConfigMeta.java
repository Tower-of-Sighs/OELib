package cc.sighs.oelib.config.model;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Configuration metadata used by IO and UI layers.
 * <p>
 * Defines storage filename, format, logical side and optional permission level.
 * A custom {@link #directory} may be set to place files in nested folders
 * under the platform config root.
 * </p>
 */
public final class ConfigMeta {
    private final Identifier id;
    private final String fileName;
    private final ConfigStorageFormat format;
    private final ConfigSide side;
    private final String directory;

    private ConfigMeta(Builder builder) {
        this.id = builder.id;
        this.fileName = builder.fileName;
        this.format = builder.format;
        this.side = builder.side;
        this.directory = builder.directory;
    }

    public static Builder builder(Identifier id) {
        Objects.requireNonNull(id);
        return new Builder(id);
    }

    public Identifier id() {
        return id;
    }

    public String fileName() {
        return fileName;
    }

    public ConfigStorageFormat format() {
        return format;
    }

    public ConfigSide side() {
        return side;
    }

    public String directory() {
        return directory;
    }

    public static final class Builder {
        private final Identifier id;
        private String fileName;
        private ConfigStorageFormat format = ConfigStorageFormat.TOML;
        private ConfigSide side = ConfigSide.SERVER;
        private String directory;

        private Builder(Identifier id) {
            this.id = id;
            this.fileName = id.getPath();
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder format(ConfigStorageFormat format) {
            this.format = format;
            return this;
        }

        public Builder side(ConfigSide side) {
            this.side = side;
            return this;
        }

        /**
         * Sets a sub-directory relative to the platform config root.
         * Accepts multi-level paths like "a/b/c".
         */
        public Builder directory(String directory) {
            this.directory = directory;
            return this;
        }

        public ConfigMeta build() {
            return new ConfigMeta(this);
        }
    }
}
