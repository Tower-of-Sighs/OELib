package cc.sighs.oelib.config.field;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Common interface for fluent field builders in a configuration schema.
 *
 * <p>A {@code FieldBuilder} accumulates metadata (comment, default value,
 * validators, migrations) and finally produces a
 * {@link RecordCodecBuilder} via {@link #forGetter(Function)} that wires
 * the field into the parent record codec.
 *
 * @param <T> the type of the field value
 */
public interface FieldBuilder<T> {

    /**
     * Sets the comment text for this field.
     *
     * @param text the comment text
     * @return this builder
     */
    FieldBuilder<T> comment(String text);

    /**
     * Enables automatic tooltip generation from the translation key.
     *
     * @return this builder
     */
    FieldBuilder<T> tooltip();

    /**
     * Sets the default value for this field.
     *
     * @param value the default value
     * @return this builder
     */
    FieldBuilder<T> defaultValue(T value);

    /**
     * Registers a validator for this field.
     *
     * @param validator a function that receives the field value and the
     *                  entire parent object, returning an error message if
     *                  validation fails
     * @return this builder
     */
    FieldBuilder<T> validate(BiFunction<T, Object, Optional<String>> validator);

    /**
     * Registers a datafix migration for this field at the given version.
     *
     * @param version   the version this migration targets
     * @param migration the migration operator
     * @return this builder
     */
    FieldBuilder<T> migrate(int version, UnaryOperator<Dynamic<?>> migration);

    /**
     * Finalizes this field and produces a record codec builder entry.
     *
     * @param getter the accessor function on the parent record
     * @param <O>    the parent record type
     * @return a record codec builder for this field
     */
    <O> RecordCodecBuilder<O, T> forGetter(Function<O, T> getter);
}
