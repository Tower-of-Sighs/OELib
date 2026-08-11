package cc.sighs.oelib.config.validation;

import java.util.Objects;

/**
 * Describes one configuration validation violation.
 *
 * @param path the serialized path of the rejected field
 * @param code the stable rule identifier
 * @param message the human-readable violation description
 * @param rejectedValue the value rejected by the rule, including {@code null}
 */
public record ConfigViolation(
        String path, String code, String message, Object rejectedValue) {
    /**
     * Validates the required violation attributes.
     *
     * @param path the serialized path of the rejected field
     * @param code the stable rule identifier
     * @param message the human-readable violation description
     * @param rejectedValue the value rejected by the rule, including {@code null}
     */
    public ConfigViolation {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
