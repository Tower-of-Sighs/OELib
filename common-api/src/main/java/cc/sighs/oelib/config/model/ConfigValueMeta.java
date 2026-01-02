package cc.sighs.oelib.config.model;

import cc.sighs.oelib.config.ui.ConfigUiHint;

import java.util.Objects;
import java.util.Optional;

/**
 * Per-field metadata collected alongside the schema codec.
 * <p>
 * Used by UI builders and TOML comment injection, not persisted in files.
 * </p>
 */
public final class ConfigValueMeta {
    private final String key;
    private final String comment;
    private final ConfigUiHint uiHint;
    private final ConfigScope scope;
    private final int permissionLevel;

    private ConfigValueMeta(Builder builder) {
        this.key = builder.key;
        this.comment = builder.comment;
        this.uiHint = builder.uiHint;
        this.scope = builder.scope;
        this.permissionLevel = builder.permissionLevel;
    }

    public static Builder builder(String key) {
        Objects.requireNonNull(key);
        return new Builder(key);
    }

    public String key() {
        return key;
    }

    public Optional<String> comment() {
        return Optional.ofNullable(comment);
    }

    public Optional<ConfigUiHint> uiHint() {
        return Optional.ofNullable(uiHint);
    }

    public ConfigScope scope() {
        return scope;
    }

    public int permissionLevel() {
        return permissionLevel;
    }

    public static final class Builder {
        private final String key;
        private String comment;
        private ConfigUiHint uiHint;
        private ConfigScope scope = ConfigScope.CLIENT;
        private int permissionLevel;

        private Builder(String key) {
            this.key = key;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder uiHint(ConfigUiHint uiHint) {
            this.uiHint = uiHint;
            return this;
        }

        public Builder scope(ConfigScope scope) {
            this.scope = scope;
            return this;
        }

        public Builder permissionLevel(int permissionLevel) {
            this.permissionLevel = permissionLevel;
            return this;
        }

        public ConfigValueMeta build() {
            return new ConfigValueMeta(this);
        }
    }
}
