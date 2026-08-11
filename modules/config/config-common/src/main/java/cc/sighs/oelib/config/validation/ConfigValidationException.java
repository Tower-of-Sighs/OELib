package cc.sighs.oelib.config.validation;

import java.util.Objects;

public final class ConfigValidationException extends IllegalStateException {
    private final ConfigValidationReport report;
    public ConfigValidationException(ConfigValidationReport report) {
        super("Configuration validation failed: "
                + Objects.requireNonNull(report, "report").summary());
        this.report = report;
    }
    public ConfigValidationReport report() {
        return report;
    }
}
