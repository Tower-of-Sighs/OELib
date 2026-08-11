package cc.sighs.oelib.config.ui;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.flechazo.hkt.Maybe;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers custom controls for generated configuration screens.
 *
 * <p>An identifier registration serves fields with a matching {@link ConfigUiHint.Custom} value.
 * A type registration serves fields of the registered value type when no identifier-specific
 * control is selected.
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
     * @return the factory, or an empty value
     */
    public static Maybe<CustomWidgetFactory> custom(ResourceLocation widgetId) {
        return Maybe.ofNullable(CUSTOM_WIDGETS.get(widgetId));
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
     * @return the factory, or an empty value
     */
    public static Maybe<TypeWidgetFactory> byType(Class<?> javaType) {
        return Maybe.ofNullable(TYPE_WIDGETS.get(javaType));
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
