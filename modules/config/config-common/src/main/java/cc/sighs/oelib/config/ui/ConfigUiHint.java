package cc.sighs.oelib.config.ui;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * Describes the control used to edit a field in a generated configuration screen.
 *
 * <p>Implementations specify numeric ranges, fixed options, standard input categories, or a
 * registered custom control. The hint does not contain a field value.
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
     */
    static Custom custom(ResourceLocation widgetId, JsonObject args) {
        Objects.requireNonNull(widgetId);
        return new Custom(widgetId, args == null ? new JsonObject() : args.deepCopy());
    }

    /**
     * A slider with a defined range and step.
     *
     * @param min the inclusive minimum value
     * @param max the inclusive maximum value
     * @param step the positive increment between selectable values
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
     *
     * @param options the selectable values in display order
     */
    record Dropdown(List<String> options) implements ConfigUiHint {
        /**
         * Creates a dropdown hint containing an immutable copy of its options.
         *
         * @param options the selectable values in display order
         */
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
     *
     * @param widgetId the registered custom widget identifier
     * @param args the widget arguments
     */
    record Custom(ResourceLocation widgetId, JsonObject args) implements ConfigUiHint {
        /**
         * Creates a custom widget hint and copies its arguments.
         *
         * @param widgetId the registered custom widget identifier
         * @param args the widget arguments, or {@code null} to use an empty object
         */
        public Custom {
            args = args == null ? new JsonObject() : args.deepCopy();
        }
    }
}
