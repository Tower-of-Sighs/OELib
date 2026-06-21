package cc.sighs.oelib.config.model;

import cc.sighs.oelib.config.ui.ConfigUiHint;
import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Per-field metadata collected during schema definition.
 *
 * <p>{@code ConfigValueMeta} is not persisted in configuration files.
 * Instead, it is consumed by the UI layer (to render the correct widget
 * for each field) and by the serialization layer (to inject TOML comments
 * and JSON5 comments). Validators and migrations attached here are applied
 * at commit time by {@link cc.sighs.oelib.config.ConfigUnit}.
 *
 * <p>Instances are created through the {@link Builder}.
 */
public final class ConfigValueMeta {
    private final String key;
    private final String comment;
    private final ConfigUiHint uiHint;
    private final String translationKey;
    private final String tooltip;
    private final boolean hidden;
    private final String visibleWhenPath;
    private final String visibleWhenValue;
    private final JsonElement defaultJsonValue;
    private final List<ConfigValueValidator> validators;
    private final List<FieldMigration> migrations;

    private ConfigValueMeta(Builder builder) {
        this.key = builder.key;
        this.comment = builder.comment;
        this.uiHint = builder.uiHint;
        this.translationKey = builder.translationKey;
        this.tooltip = builder.tooltip;
        this.hidden = builder.hidden;
        this.visibleWhenPath = builder.visibleWhenPath;
        this.visibleWhenValue = builder.visibleWhenValue;
        this.defaultJsonValue = builder.defaultJsonValue == null ? null : builder.defaultJsonValue.deepCopy();
        this.validators = List.copyOf(builder.validators);
        this.migrations = List.copyOf(builder.migrations);
    }

    /**
     * Creates a new builder for a field with the given key.
     *
     * @param key the field key
     * @return a new builder
     */
    public static Builder builder(String key) {
        Objects.requireNonNull(key);
        return new Builder(key);
    }

    /**
     * Returns the fully qualified dotted key for this field.
     *
     * @return the key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the comment text, if any.
     *
     * @return the comment, or {@link Optional#empty()}
     */
    public Optional<String> comment() {
        return Optional.ofNullable(comment);
    }

    /**
     * Returns the UI hint that determines how this field is rendered.
     *
     * @return the UI hint, or {@link Optional#empty()}
     */
    public Optional<ConfigUiHint> uiHint() {
        return Optional.ofNullable(uiHint);
    }

    /**
     * Returns the translation key for this field's label.
     *
     * @return the translation key, or {@link Optional#empty()}
     */
    public Optional<String> translationKey() {
        return Optional.ofNullable(translationKey);
    }

    /**
     * Returns the translation key for this field's tooltip.
     *
     * @return the tooltip key, or {@link Optional#empty()}
     */
    public Optional<String> tooltip() {
        return Optional.ofNullable(tooltip);
    }

    /**
     * Returns {@code true} if this field should be hidden in generated UIs.
     *
     * @return {@code true} if hidden
     */
    public boolean hidden() {
        return hidden;
    }

    /**
     * Returns the controlling path for conditional UI visibility.
     *
     * @return the controlling path, or {@link Optional#empty()} if always visible
     */
    public Optional<String> visibleWhenPath() {
        return Optional.ofNullable(visibleWhenPath);
    }

    /**
     * Returns the controlling value for conditional UI visibility.
     *
     * @return the controlling value, or {@link Optional#empty()} if always visible
     */
    public Optional<String> visibleWhenValue() {
        return Optional.ofNullable(visibleWhenValue);
    }

    /**
     * Returns the JSON default value used when this field becomes visible in
     * the generated UI.
     *
     * @return the activation default value, or {@link Optional#empty()} if none is defined
     */
    public Optional<JsonElement> defaultJsonValue() {
        return Optional.ofNullable(defaultJsonValue == null ? null : defaultJsonValue.deepCopy());
    }

    /**
     * Returns the validators attached to this field.
     *
     * @return an unmodifiable list of validators
     */
    public List<ConfigValueValidator> validators() {
        return validators;
    }

    /**
     * Returns the datafix migrations attached to this field, in declaration order.
     *
     * @return an unmodifiable list of migrations
     */
    public List<FieldMigration> migrations() {
        return migrations;
    }

    /**
     * Validates a single field value in the context of the entire configuration.
     */
    @FunctionalInterface
    public interface ConfigValueValidator {
        /**
         * Validates the given field value.
         *
         * @param fieldValue    the value of the field being validated
         * @param entireConfig  the complete configuration value for cross-field checks
         * @return an {@link Optional} containing an error message if validation fails,
         *         or {@link Optional#empty()} if the value is valid
         */
        Optional<String> validate(Object fieldValue, Object entireConfig);
    }

    /**
     * Mutable builder for {@link ConfigValueMeta}.
     */
    public static final class Builder {
        private final String key;
        private String comment;
        private ConfigUiHint uiHint;
        private String translationKey;
        private String tooltip;
        private boolean hidden;
        private String visibleWhenPath;
        private String visibleWhenValue;
        private JsonElement defaultJsonValue;
        private final List<ConfigValueValidator> validators = new ArrayList<>();
        private final List<FieldMigration> migrations = new ArrayList<>();

        private Builder(String key) {
            this.key = key;
        }

        /**
         * Sets the comment text.
         *
         * @param comment the comment, or {@code null} to clear
         * @return this builder
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Sets the UI hint for auto-generated screens.
         *
         * @param uiHint the UI hint, or {@code null} to clear
         * @return this builder
         */
        public Builder uiHint(ConfigUiHint uiHint) {
            this.uiHint = uiHint;
            return this;
        }

        /**
         * Sets the translation key for the field label.
         *
         * @param translationKey the translation key, or {@code null} to clear
         * @return this builder
         */
        public Builder translationKey(String translationKey) {
            this.translationKey = translationKey;
            return this;
        }

        /**
         * Sets the translation key for the tooltip.
         *
         * @param tooltip the tooltip key, or {@code null} to clear
         * @return this builder
         */
        public Builder tooltip(String tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        /**
         * Sets whether this field is hidden in generated UIs.
         *
         * @param hidden {@code true} to hide the field
         * @return this builder
         */
        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        /**
         * Sets the controlling path for conditional UI visibility.
         *
         * @param visibleWhenPath the controlling path, or {@code null} to clear
         * @return this builder
         */
        public Builder visibleWhenPath(String visibleWhenPath) {
            this.visibleWhenPath = visibleWhenPath;
            return this;
        }

        /**
         * Sets the controlling value for conditional UI visibility.
         *
         * @param visibleWhenValue the controlling value, or {@code null} to clear
         * @return this builder
         */
        public Builder visibleWhenValue(String visibleWhenValue) {
            this.visibleWhenValue = visibleWhenValue;
            return this;
        }

        /**
         * Sets the JSON default value used when this field becomes visible in
         * the generated UI.
         *
         * @param defaultJsonValue the activation default value, or {@code null} to clear
         * @return this builder
         */
        public Builder defaultJsonValue(JsonElement defaultJsonValue) {
            this.defaultJsonValue = defaultJsonValue == null ? null : defaultJsonValue.deepCopy();
            return this;
        }

        /**
         * Adds a validator.
         *
         * @param validator the validator to add
         * @return this builder
         */
        public Builder validator(ConfigValueValidator validator) {
            this.validators.add(validator);
            return this;
        }

        /**
         * Adds all validators from the given list.
         *
         * @param validators the validators to add
         * @return this builder
         */
        public Builder validators(List<ConfigValueValidator> validators) {
            this.validators.addAll(validators);
            return this;
        }

        /**
         * Adds a field-level datafix migration.
         *
         * @param version   the version this migration targets
         * @param migration the migration operator
         * @return this builder
         */
        public Builder migration(int version, UnaryOperator<Dynamic<?>> migration) {
            this.migrations.add(new FieldMigration(version, migration));
            return this;
        }

        /**
         * Adds all migrations from the given list.
         *
         * @param migrations the migrations to add
         * @return this builder
         */
        public Builder migrations(List<FieldMigration> migrations) {
            this.migrations.addAll(migrations);
            return this;
        }

        /**
         * Builds the metadata instance.
         *
         * @return the new {@link ConfigValueMeta}
         */
        public ConfigValueMeta build() {
            return new ConfigValueMeta(this);
        }
    }

    /**
     * A versioned, field-level datafix migration.
     *
     * @param version   the version of this migration
     * @param migration the operator that transforms the field's dynamic representation
     */
    public record FieldMigration(int version, UnaryOperator<Dynamic<?>> migration) {
        public FieldMigration {
            Objects.requireNonNull(migration);
        }
    }
}
