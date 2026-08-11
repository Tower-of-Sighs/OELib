package cc.sighs.oelib.config.field;

import com.flechazo.optics.LensGetter;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Defines the common contract for configuration field builders.
 *
 * <p>A field builder associates serialization metadata, validation rules, and raw-data migrations
 * with one record component. Calling {@link #forGetter(LensGetter)} completes the field definition.
 *
 * @param <T> the type of the field value
 */
public interface FieldBuilder<T> {

    /**
     * Sets the comment associated with the serialized field and generated configuration screen.
     *
     * @param text the comment to associate with the field
     * @return this builder instance
     */
    FieldBuilder<T> comment(String text);

    /**
     * Enables a tooltip key derived from the field translation key.
     *
     * @return this builder instance
     */
    FieldBuilder<T> tooltip();

    /**
     * Sets the value returned by the field codec when the serialized field is absent.
     *
     * @param value the value to use for an absent field
     * @return this builder instance
     */
    FieldBuilder<T> defaultValue(T value);

    /**
     * Registers a validation rule for this field.
 *
     * <p>The rule returns an empty value when validation succeeds and a message when validation
     * fails. All registered rules are evaluated when the containing configuration is validated.
     *
     * @param code the stable identifier reported for a violation
     * @param validator the rule evaluated against the field value
     * @return this builder instance
     */
    FieldBuilder<T> validate(String code, Function<T, Optional<String>> validator);

    /**
     * Registers a validation rule that may inspect the complete configuration value.
     *
     * <p>The supplied root type is checked before the rule is invoked. The rule returns an empty
     * value when validation succeeds and a message when validation fails.
     *
     * @param code the stable identifier reported for a violation
     * @param rootClass the runtime type of the complete configuration value
     * @param validator the rule evaluated against the field and complete configuration values
     * @param <R> the complete configuration type
     * @return this builder instance
     * @throws ClassCastException if the validated configuration is not an instance of
     *         {@code rootClass}
     */
    <R> FieldBuilder<T> validateRoot(
            String code, Class<R> rootClass, BiFunction<T, R, Optional<String>> validator);

    /**
     * Registers a raw-data migration for this field.
 *
     * <p>The migration participates in the transition from {@code fromVersion} to
     * {@code toVersion} and runs before the current field codec decodes the value.
     *
     * @param fromVersion the nonnegative source version
     * @param toVersion the target version, greater than {@code fromVersion}
     * @param migration the transformation applied to the field's dynamic representation
     * @return this builder instance
     * @throws IllegalArgumentException if either version is outside the permitted range
     */
    FieldBuilder<T> migrate(
            int fromVersion, int toVersion, UnaryOperator<Dynamic<?>> migration);

    /**
     * Creates a record codec entry and registers the field with the active schema definition.
 *
     * @param getter the record component accessor associated with this field
     * @param <O> the record type containing the field
     * @return a codec builder entry for the containing record
     * @throws IllegalArgumentException if {@code getter} does not identify a record component
     * @throws IllegalStateException if no schema definition is active
     */
    <O> RecordCodecBuilder<O, T> forGetter(LensGetter<O, T> getter);
}
