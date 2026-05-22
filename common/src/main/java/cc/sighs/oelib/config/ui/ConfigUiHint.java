package cc.sighs.oelib.config.ui;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * A sealed interface describing how a configuration field is rendered in
 * the auto-generated config screen.
 *
 * <p>Each variant maps to a widget type:
 * <ul>
 *   <li>{@link Slider} — a numeric slider with configurable range and step</li>
 *   <li>{@link Text} — a text input field</li>
 *   <li>{@link Toggle} — a checkbox for boolean values</li>
 *   <li>{@link Dropdown} — a cycle button with predefined options</li>
 *   <li>{@link Group} — a visual grouping element</li>
 *   <li>{@link Custom} — a custom widget registered via
 *       {@link ConfigWidgetRegistry}</li>
 * </ul>
 *
 * <p>Hints are created through the static factory methods and attached to
 * fields via {@link cc.sighs.oelib.config.model.ConfigValueMeta.Builder#uiHint(ConfigUiHint)
 * ConfigValueMeta.Builder.uiHint}.
 */
public sealed interface ConfigUiHint permits ConfigUiHint.Slider, ConfigUiHint.Text, ConfigUiHint.Toggle,
        ConfigUiHint.Dropdown, ConfigUiHint.Group, ConfigUiHint.Custom {

    /**
     * Creates a slider hint for a numeric range.
     *
     * @param min  the minimum value (inclusive)
     * @param max  the maximum value (inclusive)
     * @param step the step increment
     * @return a slider hint
     */
    static Slider slider(double min, double max, double step) {
        return new Slider(min, max, step);
    }

    /**
     * Creates a text input hint.
     *
     * @return a text hint
     */
    static Text text() {
        return new Text();
    }

    /**
     * Creates a toggle (checkbox) hint for boolean fields.
     *
     * @return a toggle hint
     */
    static Toggle toggle() {
        return new Toggle();
    }

    /**
     * Creates a dropdown hint with the given options.
     *
     * @param options the list of option strings
     * @return a dropdown hint
     * @throws NullPointerException if {@code options} is {@code null}
     */
    static Dropdown dropdown(List<String> options) {
        Objects.requireNonNull(options);
        return new Dropdown(List.copyOf(options));
    }

    /**
     * Creates a visual group hint.
     *
     * @return a group hint
     */
    static Group group() {
        return new Group();
    }

    /**
     * Creates a custom widget hint referencing a registered widget factory.
     *
     * @param widgetId the id of the widget registered in
     *                 {@link ConfigWidgetRegistry}
     * @param args     optional arguments for the widget, or {@code null}
     * @return a custom hint
     * @throws NullPointerException if {@code widgetId} is {@code null}
     */
    static Custom custom(ResourceLocation widgetId, JsonObject args) {
        Objects.requireNonNull(widgetId);
        return new Custom(widgetId, args == null ? new JsonObject() : args.deepCopy());
    }

    /**
     * A slider with a defined range and step.
     */
    record Slider(double min, double max, double step) implements ConfigUiHint {
    }

    /**
     * A plain text input field.
     */
    record Text() implements ConfigUiHint {
    }

    /**
     * A toggle checkbox for boolean values.
     */
    record Toggle() implements ConfigUiHint {
    }

    /**
     * A dropdown selector with a fixed set of options.
     */
    record Dropdown(List<String> options) implements ConfigUiHint {
        public Dropdown {
            options = List.copyOf(options);
        }
    }

    /**
     * A visual grouping marker.
     */
    record Group() implements ConfigUiHint {
    }

    /**
     * A reference to a custom widget registered via
     * {@link ConfigWidgetRegistry#register(ResourceLocation, ConfigWidgetRegistry.CustomWidgetFactory)}.
     */
    record Custom(ResourceLocation widgetId, JsonObject args) implements ConfigUiHint {
        public Custom {
            args = args == null ? new JsonObject() : args.deepCopy();
        }
    }
}
