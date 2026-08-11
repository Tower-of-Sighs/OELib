package cc.sighs.oelib.config.validation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record ConfigValidationReport(List<ConfigViolation> violations) {

    public ConfigValidationReport {
        violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        if (violations.isEmpty()) {
            throw new IllegalArgumentException("A failed validation report must not be empty");
        }
    }

    public String summary() {
        return violations.stream()
                .map(violation -> violation.path() + " [" + violation.code() + "]: "
                        + violation.message())
                .collect(Collectors.joining("; "));
    }
}
