package cc.sighs.oelib.config.model;

/**
 * The serialization format used to read and write configuration files.
 */
public enum ConfigStorageFormat {
    /** Standard JSON with pretty printing. */
    JSON,
    /** JSON5 with comment support. */
    JSON5,
    /** TOML with comment injection for field metadata. */
    TOML
}
