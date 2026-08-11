package cc.sighs.oelib.data.api;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking expression functions.
 * <p>
 * Static methods annotated with this annotation will be automatically registered into the expression engine,
 * and can be used in condition expressions and action expressions within data packs.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @ExpressionFunction(value = "isHoldingItem", description = "Checks whether the player is holding the specified item")
 * public static boolean isHoldingItem(String itemId) {
 *     // implementation logic
 *     return false;
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiStatus.Internal
public @interface ExpressionFunction {

    /**
     * The name of the function as it appears in expressions.
     * <p>
     * If left as an empty string, the method name will be used as the function name.
     * Function names must be globally unique; duplicate names will cause registration to fail.
     * </p>
     *
     * @return the function name, defaulting to an empty string
     */
    String value() default "";

    /**
     * A description of the function.
     * <p>
     * Used for documentation generation and debug logs. It is recommended to provide a clear functional description.
     * </p>
     *
     * @return the function description, defaulting to an empty string
     */
    String description() default "";

    /**
     * The category of the function.
     * <p>
     * Used for organizing and managing functions, facilitating documentation generation and debugging.
     * </p>
     *
     * @return the function category, defaulting to "general"
     */
    String category() default "general";
}