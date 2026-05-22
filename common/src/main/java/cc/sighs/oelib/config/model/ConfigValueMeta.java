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
    private final String translationKey;
    private final String tooltip;
    private final boolean hidden;

    private ConfigValueMeta(Builder builder) {
        this.key = builder.key;
        this.comment = builder.comment;
        this.uiHint = builder.uiHint;
        this.translationKey = builder.translationKey;
        this.tooltip = builder.tooltip;
        this.hidden = builder.hidden;
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

    public Optional<String> translationKey() {
        return Optional.ofNullable(translationKey);
    }

    public Optional<String> tooltip() {
        return Optional.ofNullable(tooltip);
    }

    public boolean hidden() {
        return hidden;
    }

    public static final class Builder {
        private final String key;
        private String comment;
        private ConfigUiHint uiHint;
        private String translationKey;
        private String tooltip;
        private boolean hidden;

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

        public Builder translationKey(String translationKey) {
            this.translationKey = translationKey;
            return this;
        }

        public Builder tooltip(String tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public ConfigValueMeta build() {
            return new ConfigValueMeta(this);
        }
    }
}
