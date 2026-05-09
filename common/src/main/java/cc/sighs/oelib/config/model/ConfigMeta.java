package cc.sighs.oelib.config.model;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Configuration metadata consumed by the I/O and UI layers.
 *
 * <p>An {@code ConfigMeta} instance describes where and how a configuration
 * is persisted: its storage filename, serialization format, logical side
 * (client or server), and an optional sub-directory under the platform
 * config root.
 *
 * <p>Instances are created through the {@link Builder}, which is obtained via
 * {@link #builder(Identifier)}.
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

    /**
     * Creates a new builder for the given configuration identifier.
     *
     * @param id the configuration identifier
     * @return a new builder
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public static Builder builder(Identifier id) {
        Objects.requireNonNull(id);
        return new Builder(id);
    }

    /**
     * Returns the configuration identifier.
     *
     * @return the identifier
     */
    public Identifier id() {
        return id;
    }

    /**
     * Returns the storage filename, without directories.
     *
     * @return the filename
     */
    public String fileName() {
        return fileName;
    }

    /**
     * Returns the serialization format.
     *
     * @return the format
     */
    public ConfigStorageFormat format() {
        return format;
    }

    /**
     * Returns the logical side this configuration belongs to.
     *
     * @return the side
     */
    public ConfigSide side() {
        return side;
    }

    /**
     * Returns the sub-directory under the platform config root,
     * or {@code null} for the root directory.
     *
     * @return the directory path, or {@code null}
     */
    public String directory() {
        return directory;
    }

    /**
     * Mutable builder for {@link ConfigMeta}.
     *
     * <p>Defaults: format is {@link ConfigStorageFormat#TOML TOML}, side is
     * {@link ConfigSide#SERVER SERVER}, filename is derived from the
     * identifier's path component.
     */
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

        /**
         * Sets the storage filename.
         *
         * @param fileName the filename, without directory components
         * @return this builder
         */
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * Sets the serialization format.
         *
         * @param format the format
         * @return this builder
         */
        public Builder format(ConfigStorageFormat format) {
            this.format = format;
            return this;
        }

        /**
         * Sets the logical side.
         *
         * @param side the side
         * @return this builder
         */
        public Builder side(ConfigSide side) {
            this.side = side;
            return this;
        }

        /**
         * Sets a sub-directory relative to the platform config root.
         * Multi-level paths (for example {@code "a/b/c"}) are accepted.
         *
         * @param directory the sub-directory path, or {@code null} to use the root
         * @return this builder
         */
        public Builder directory(String directory) {
            this.directory = directory;
            return this;
        }

        /**
         * Builds the metadata instance.
         *
         * @return the new {@link ConfigMeta}
         */
        public ConfigMeta build() {
            return new ConfigMeta(this);
        }
    }
}
