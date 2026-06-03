package cc.sighs.oelib.config.ui;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for custom configuration UI widgets.
 *
 * <p>Widgets can be registered in two ways:
 * <ul>
 *   <li>By {@link ResourceLocation} — a specific widget factory that is
 *       referenced from a {@link ConfigUiHint.Custom} hint.</li>
 *   <li>By Java type — a fallback factory that is applied to all fields
 *       of a given class.</li>
 * </ul>
 *
 * <p>Factories are invoked when the config screen creates controls for
 * each field. The resulting {@link CustomWidgetHandle} provides the
 * actual {@link AbstractWidget} to be rendered.
 */
public final class ConfigWidgetRegistry {
    private static final Map<ResourceLocation, CustomWidgetFactory> CUSTOM_WIDGETS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, TypeWidgetFactory> TYPE_WIDGETS = new ConcurrentHashMap<>();

    private ConfigWidgetRegistry() {
    }

    /**
     * Registers a custom widget factory keyed by id.
     *
     * @param widgetId the widget id
     * @param factory  the factory
     */
    public static void register(ResourceLocation widgetId, CustomWidgetFactory factory) {
        CUSTOM_WIDGETS.put(widgetId, factory);
    }

    /**
     * Looks up a custom widget factory by id.
     *
     * @param widgetId the widget id
     * @return the factory, or {@link Optional#empty()}
     */
    public static Optional<CustomWidgetFactory> custom(ResourceLocation widgetId) {
        return Optional.ofNullable(CUSTOM_WIDGETS.get(widgetId));
    }

    /**
     * Registers a type-based widget factory.
     *
     * @param javaType the Java type to bind
     * @param factory  the factory
     */
    public static void register(Class<?> javaType, TypeWidgetFactory factory) {
        TYPE_WIDGETS.put(javaType, factory);
    }

    /**
     * Looks up a type-based widget factory.
     *
     * @param javaType the Java type
     * @return the factory, or {@link Optional#empty()}
     */
    public static Optional<TypeWidgetFactory> byType(Class<?> javaType) {
        return Optional.ofNullable(TYPE_WIDGETS.get(javaType));
    }

    /**
     * Factory interface for creating custom widgets identified by a
     * {@link ResourceLocation}.
     */
    public interface CustomWidgetFactory {
        /**
         * Creates a widget handle from the given context.
         *
         * @param context the widget context
         * @return a handle providing the widget
         */
        CustomWidgetHandle create(CustomWidgetContext context);
    }

    /**
     * Factory interface for creating custom widgets for a specific Java type.
     */
    public interface TypeWidgetFactory {
        /**
         * Creates a widget handle from the given context.
         *
         * @param context the widget context
         * @return a handle providing the widget
         */
        CustomWidgetHandle create(CustomWidgetContext context);
    }

    /**
     * Handle returned by widget factories, providing the actual widget
     * and an optional reset callback.
     */
    public interface CustomWidgetHandle {
        /**
         * Returns the widget to be rendered on the screen.
         *
         * @return the widget
         */
        AbstractWidget widget();

        /**
         * Called when the user resets the field to its default value.
         *
         * @param value the default value
         */
        default void reset(JsonElement value) {
        }
    }

    /**
     * Context passed to widget factories at creation time.
     *
     * @param screen         the parent config screen
     * @param meta           the field metadata
     * @param working        the working JSON object being edited
     * @param currentValue   the current value of the field
     * @param x              the x position for the widget
     * @param y              the y position for the widget
     * @param width          the width for the widget
     * @param height         the height for the widget
     * @param onValueChanged a callback to invoke when the widget value changes
     */
    public record CustomWidgetContext(
            Screen screen,
            ConfigValueMeta meta,
            JsonObject working,
            JsonElement currentValue,
            int x,
            int y,
            int width,
            int height,
            Runnable onValueChanged
    ) {
    }
}
