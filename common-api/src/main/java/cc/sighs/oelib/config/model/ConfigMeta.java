package cc.sighs.oelib.config.model;

import net.minecraft.resources.ResourceLocation;

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
    private final ResourceLocation id;
    private final String fileName;
    private final ConfigStorageFormat format;
    private final ConfigSide side;
    private final int permissionLevel;
    private final String directory;

    private ConfigMeta(Builder builder) {
        this.id = builder.id;
        this.fileName = builder.fileName;
        this.format = builder.format;
        this.side = builder.side;
        this.permissionLevel = builder.permissionLevel;
        this.directory = builder.directory;
    }

    public static Builder builder(ResourceLocation id) {
        Objects.requireNonNull(id);
        return new Builder(id);
    }

    public ResourceLocation id() {
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

    public int permissionLevel() {
        return permissionLevel;
    }

    public String directory() {
        return directory;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private String fileName;
        private ConfigStorageFormat format = ConfigStorageFormat.TOML;
        private ConfigSide side = ConfigSide.BOTH;
        private int permissionLevel;
        private String directory;

        private Builder(ResourceLocation id) {
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

        public Builder permissionLevel(int permissionLevel) {
            this.permissionLevel = permissionLevel;
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
