package cc.sighs.oelib.config.ui;

import java.util.List;
import java.util.Objects;

/**
 * UI hint attached to a field for auto-generated configuration screens.
 * <p>
 * Encodes control type and parameters such as slider ranges or text inputs.
 * </p>
 */
public final class ConfigUiHint {
    private final ConfigUiType type;
    private final Double min;
    private final Double max;
    private final Double step;
    private final List<String> options;
    private final String customRenderer;

    private ConfigUiHint(ConfigUiType type, Double min, Double max, Double step, List<String> options, String customRenderer) {
        this.type = type;
        this.min = min;
        this.max = max;
        this.step = step;
        this.options = options;
        this.customRenderer = customRenderer;
    }

    public static ConfigUiHint slider(double min, double max, double step) {
        return new ConfigUiHint(ConfigUiType.SLIDER, min, max, step, null, null);
    }

    public static ConfigUiHint text() {
        return new ConfigUiHint(ConfigUiType.TEXT, null, null, null, null, null);
    }

    public static ConfigUiHint toggle() {
        return new ConfigUiHint(ConfigUiType.TOGGLE, null, null, null, null, null);
    }

    public static ConfigUiHint dropdown(List<String> options) {
        Objects.requireNonNull(options);
        return new ConfigUiHint(ConfigUiType.DROPDOWN, null, null, null, options, null);
    }

    public static ConfigUiHint colorPicker() {
        return new ConfigUiHint(ConfigUiType.COLOR_PICKER, null, null, null, null, null);
    }

    public static ConfigUiHint custom(String rendererId) {
        Objects.requireNonNull(rendererId);
        return new ConfigUiHint(ConfigUiType.CUSTOM, null, null, null, null, rendererId);
    }

    public ConfigUiType type() {
        return type;
    }

    public Double min() {
        return min;
    }

    public Double max() {
        return max;
    }

    public Double step() {
        return step;
    }

    public List<String> options() {
        return options;
    }

    public String customRenderer() {
        return customRenderer;
    }
}
