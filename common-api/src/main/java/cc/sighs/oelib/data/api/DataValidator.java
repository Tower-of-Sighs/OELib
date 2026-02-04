package cc.sighs.oelib.data.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * Interface for data validators.
 * <p>
 * Implement this interface to provide custom validation logic for data-driven types.
 * </p>
 *
 * @param <T> the data type
 */
public interface DataValidator<T> {

    /**
     * Validates the correctness of the given data.
     *
     * @param data   the data to validate
     * @param source the file location from which the data originates
     * @return the validation result
     */
    ValidationResult validate(T data, ResourceLocation source);

    /**
     * Context-aware data validator interface.
     * <p>
     * Validators implementing this interface can access the server instance,
     * enabling them to use contextual information such as registry lookups.
     * </p>
     *
     * @param <T> the data type
     */
    interface ServerContextAware<T> extends DataValidator<T> {

        /**
         * Validates data using server context.
         *
         * @param data   the data to validate
         * @param source the file location from which the data originates
         * @param server the server instance (may be null, e.g., on client side)
         * @return the validation result
         */
        ValidationResult validateWithContext(T data, ResourceLocation source, MinecraftServer server);

        /**
         * Default implementation: uses context-aware validation if a server instance is available;
         * otherwise falls back to basic validation.
         */
        @Override
        default ValidationResult validate(T data, ResourceLocation source) {
            return validateWithContext(data, source, null);
        }
    }

    /**
     * Represents the result of a validation.
     */
    record ValidationResult(boolean valid, String message, boolean deferrable) {

        /**
         * Creates a successful validation result.
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null, false);
        }

        /**
         * Creates a failed validation result.
         *
         * @param message the error message
         */
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, false);
        }

        /**
         * Creates a deferred validation result.
         * <p>
         * Used when validation depends on runtime state (e.g., tag systems).
         * The data will be loaded but marked for deferred validation.
         * </p>
         *
         * @param message the reason for deferral
         */
        public static ValidationResult deferred(String message) {
            return new ValidationResult(true, message, true);
        }
    }

    /**
     * Default no-op validator implementation.
     */
    class NoValidator implements DataValidator<Object> {
        @Override
        public ValidationResult validate(Object data, ResourceLocation source) {
            return ValidationResult.success();
        }
    }
}