package cc.sighs.oelib.config.model;

import cc.sighs.oelib.config.ui.ConfigUiHint;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.business.data.NonEmptyList;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Describes one serialized configuration field.
 *
 * <p>The descriptor contains the field's serialized path, presentation metadata, validation
 * rules, raw-data migrations, and root-value reader. Instances are immutable and are created by
 * {@link #builder(String)}.
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
    private final Codec<?> valueCodec;
    private final Object defaultValue;
    private final List<ConfigValueValidator> validators;
    private final List<FieldMigration> migrations;
    private final String[] pathSegments;
    private final Function<Object, Object> accessor;

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
        this.valueCodec = builder.valueCodec;
        this.defaultValue = builder.defaultValue;
        this.validators = List.copyOf(builder.validators);
        this.migrations = List.copyOf(builder.migrations);
        this.pathSegments = builder.key.split("\\.");
        this.accessor = builder.accessor;
    }

    /**
     * Creates a metadata builder for the specified serialized field path.
     *
     * @param key the nonempty dotted path that identifies the field
     * @return a new metadata builder
     */
    public static Builder builder(String key) {
        Objects.requireNonNull(key);
        return new Builder(key);
    }

    /**
     * Returns the fully qualified serialized path for this field.
     *
     * @return the dotted field path
     */
    public String key() {
        return key;
    }

    /**
     * Returns the comment associated with this field.
     *
     * @return the comment, or an empty value when no comment is defined
     */
    public Maybe<String> comment() {
        return Maybe.ofNullable(comment);
    }

    /**
     * Returns the presentation hint associated with this field.
     *
     * @return the presentation hint, or an empty value when none is defined
     */
    public Maybe<ConfigUiHint> uiHint() {
        return Maybe.ofNullable(uiHint);
    }

    /**
     * Returns the translation key for this field's label.
     *
     * @return the label translation key, or an empty value when none is defined
     */
    public Maybe<String> translationKey() {
        return Maybe.ofNullable(translationKey);
    }

    /**
     * Returns the translation key for this field's tooltip.
     *
     * @return the tooltip translation key, or an empty value when none is defined
     */
    public Maybe<String> tooltip() {
        return Maybe.ofNullable(tooltip);
    }

    /**
     * Determines whether generated configuration screens omit this field.
     *
     * @return {@code true} when the field is omitted; otherwise {@code false}
     */
    public boolean hidden() {
        return hidden;
    }

    /**
     * Returns the field path that controls conditional visibility.
     *
     * @return the controlling path, or an empty value when visibility is unconditional
     */
    public Maybe<String> visibleWhenPath() {
        return Maybe.ofNullable(visibleWhenPath);
    }

    /**
     * Returns the serialized value that enables conditional visibility.
     *
     * @return the enabling value, or an empty value when visibility is unconditional
     */
    public Maybe<String> visibleWhenValue() {
        return Maybe.ofNullable(visibleWhenValue);
    }

    /**
     * Returns the JSON value assigned when a conditionally visible field is enabled.
     *
     * @return a copy of the activation value, or an empty value when none is defined
     */
    public Maybe<JsonElement> defaultJsonValue() {
        return Maybe.ofNullable(defaultJsonValue == null ? null : defaultJsonValue.deepCopy());
    }

    /**
     * Returns the codec associated with this field.
     *
     * @return the field codec, or an empty value when the descriptor is presentation-only
     */
    public Maybe<Codec<?>> valueCodec() {
        return Maybe.ofNullable(valueCodec);
    }

    /**
     * Returns the value used when the serialized field is absent.
     *
     * @return the field default, or an empty value when no default is defined
     */
    public Maybe<Object> defaultValue() {
        return Maybe.ofNullable(defaultValue);
    }

    /**
     * Returns the validation rules in registration order.
     *
     * @return an unmodifiable list of validation rules
     */
    public List<ConfigValueValidator> validators() {
        return validators;
    }

    /**
     * Returns the raw-data migrations in registration order.
     *
     * @return an unmodifiable list of field migrations
     */
    public List<FieldMigration> migrations() {
        return migrations;
    }

    /**
     * Returns the components of the serialized field path.
     *
     * @return an unmodifiable list of nonempty path components
     */
    public List<String> pathSegments() {
        return List.of(pathSegments.clone());
    }

    /**
     * Returns this field's value from a complete configuration value.
     *
     * @param root the complete configuration value
     * @return the field value, including {@code null} when the configured reader returns it
     * @throws IllegalStateException if no field reader is associated with this descriptor
     */
    public Object read(Object root) {
        if (accessor == null) {
            throw new IllegalStateException("No compiled field focus for " + key);
        }
        return accessor.apply(root);
    }

    /**
     * Determines whether this descriptor can read its field from a complete configuration value.
     *
     * @return {@code true} when a field reader is present; otherwise {@code false}
     */
    public boolean hasReader() {
        return accessor != null;
    }

    /**
     * Validates a field value in the context of a complete configuration value.
     */
    @FunctionalInterface
    public interface ConfigValueValidator {
        /**
         * Validates the specified field value.
         *
         * @param fieldValue the field value to validate
         * @param entireConfig the complete configuration value
         * @return an empty value when validation succeeds, or a value describing the failed rule
         */
        Validated<NonEmptyList<ValidationFailure>, Object> validate(
                Object fieldValue, Object entireConfig);
    }

    /**
     * Describes a failed field validation rule before the rejected value and field path are added.
     *
     * @param code the stable rule identifier
     * @param message the human-readable violation description
     */
    public record ValidationFailure(String code, String message) {
        /**
         * Validates the required failure attributes.
         *
         * @param code the stable rule identifier
         * @param message the human-readable violation description
         */
        public ValidationFailure {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }

    /**
     * Builds an immutable field descriptor.
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
        private Codec<?> valueCodec;
        private Object defaultValue;
        private final List<ConfigValueValidator> validators = new ArrayList<>();
        private final List<FieldMigration> migrations = new ArrayList<>();
        private Function<Object, Object> accessor;

        private Builder(String key) {
            this.key = key;
        }

        /**
         * Sets the comment associated with the field.
         *
         * @param comment the comment, or {@code null} to clear
         * @return this builder instance
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Sets the presentation hint for generated configuration screens.
         *
         * @param uiHint the UI hint, or {@code null} to clear
         * @return this builder instance
         */
        public Builder uiHint(ConfigUiHint uiHint) {
            this.uiHint = uiHint;
            return this;
        }

        /**
         * Sets the translation key for the field label.
         *
         * @param translationKey the translation key, or {@code null} to clear
         * @return this builder instance
         */
        public Builder translationKey(String translationKey) {
            this.translationKey = translationKey;
            return this;
        }

        /**
         * Sets the translation key for the tooltip.
         *
         * @param tooltip the tooltip key, or {@code null} to clear
         * @return this builder instance
         */
        public Builder tooltip(String tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        /**
         * Sets whether generated configuration screens omit this field.
         *
         * @param hidden {@code true} to omit the field; {@code false} to include it
         * @return this builder instance
         */
        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        /**
         * Sets the controlling path for conditional UI visibility.
         *
         * @param visibleWhenPath the controlling path, or {@code null} to clear
         * @return this builder instance
         */
        public Builder visibleWhenPath(String visibleWhenPath) {
            this.visibleWhenPath = visibleWhenPath;
            return this;
        }

        /**
         * Sets the controlling value for conditional UI visibility.
         *
         * @param visibleWhenValue the controlling value, or {@code null} to clear
         * @return this builder instance
         */
        public Builder visibleWhenValue(String visibleWhenValue) {
            this.visibleWhenValue = visibleWhenValue;
            return this;
        }

        /**
         * Sets the JSON value assigned when a conditionally visible field is enabled.
         *
         * @param defaultJsonValue the activation default value, or {@code null} to clear
         * @return this builder instance
         */
        public Builder defaultJsonValue(JsonElement defaultJsonValue) {
            this.defaultJsonValue = defaultJsonValue == null ? null : defaultJsonValue.deepCopy();
            return this;
        }

        /**
         * Sets the codec associated with this field.
         *
         * @param valueCodec the field codec, or {@code null} for presentation-only metadata
         * @return this builder instance
         */
        public Builder valueCodec(Codec<?> valueCodec) {
            this.valueCodec = valueCodec;
            return this;
        }

        /**
         * Sets the value used when the serialized field is absent.
         *
         * @param defaultValue the field default, or {@code null} to remove the current default
         * @return this builder instance
         */
        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Adds a validation rule after previously registered rules.
         *
         * @param validator the rule to register
         * @return this builder instance
         */
        public Builder validator(ConfigValueValidator validator) {
            this.validators.add(validator);
            return this;
        }

        /**
         * Adds validation rules in list encounter order.
         *
         * @param validators the rules to register
         * @return this builder instance
         */
        public Builder validators(List<ConfigValueValidator> validators) {
            this.validators.addAll(validators);
            return this;
        }

        /**
         * Sets the function used to read the field from a complete configuration value.
         *
         * @param accessor the field reader, or {@code null} to remove the current reader
         * @return this builder instance
         */
        public Builder accessor(Function<Object, Object> accessor) {
            this.accessor = accessor;
            return this;
        }

        /**
         * Adds a raw-data migration for this field.
         *
         * @param fromVersion the nonnegative source version
         * @param toVersion the target version, greater than {@code fromVersion}
         * @param migration the transformation applied before the current field codec decodes
         * @return this builder instance
         * @throws IllegalArgumentException if either version is outside the permitted range
         */
        public Builder migration(
                int fromVersion, int toVersion, UnaryOperator<Dynamic<?>> migration) {
            this.migrations.add(new FieldMigration(fromVersion, toVersion, migration));
            return this;
        }

        /**
         * Adds field migrations in list encounter order.
         *
         * @param migrations the migrations to add
         * @return this builder instance
         */
        public Builder migrations(List<FieldMigration> migrations) {
            this.migrations.addAll(migrations);
            return this;
        }

        /**
         * Creates an immutable descriptor from the current builder state.
         *
         * @return the completed field descriptor
         */
        public ConfigValueMeta build() {
            return new ConfigValueMeta(this);
        }
    }

    /**
     * Describes one version transition for a field's raw dynamic representation.
     *
     * @param fromVersion the nonnegative source version
     * @param toVersion the target version, greater than {@code fromVersion}
     * @param migration the transformation applied to the field's raw value
     */
    public record FieldMigration(
            int fromVersion, int toVersion, UnaryOperator<Dynamic<?>> migration) {
        /**
         * Validates the migration version transition.
         *
         * @param fromVersion the nonnegative source version
         * @param toVersion the target version, greater than {@code fromVersion}
         * @param migration the transformation applied to the field's raw value
         * @throws IllegalArgumentException if the version transition does not advance from a
         *         nonnegative source version
         */
        public FieldMigration {
            Objects.requireNonNull(migration);
            if (fromVersion < 0 || toVersion <= fromVersion) {
                throw new IllegalArgumentException(
                        "Field migration must advance from a non-negative version");
            }
        }
    }
}
