package cc.sighs.oelib.config.util;

import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.OELibConfig;
import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.util.OptionalOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * GUI helpers for the auto-generated configuration screen.
 */
public final class ConfigGuiUtil {

    private ConfigGuiUtil() {
    }

    /**
     * Retrieves the JSON element at the given dotted path within a JSON object.
     *
     * @param obj  the root JSON object
     * @param path the dotted path
     * @return the element at the path, or {@code null} if the path does not exist
     */
    public static JsonElement getPath(JsonObject obj, String path) {
        if (obj == null || path == null) return null;
        var parts = path.split("\\.");
        var cur = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            String p = parts[i];
            if (!cur.has(p) || !cur.get(p).isJsonObject()) {
                return null;
            }
            cur = cur.getAsJsonObject(p);
        }
        var last = parts[parts.length - 1];
        return cur.has(last) ? cur.get(last) : null;
    }

    /**
     * Sets the JSON element at the given dotted path, creating intermediate
     * objects as needed.
     *
     * @param obj   the root JSON object (mutated in place)
     * @param path  the dotted path
     * @param value the value to set
     */
    public static void setPath(JsonObject obj, String path, JsonElement value) {
        var parts = path.split("\\.");
        var cur = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            var p = parts[i];
            if (!cur.has(p) || !cur.get(p).isJsonObject()) {
                JsonObject next = new JsonObject();
                cur.add(p, next);
                cur = next;
            } else {
                cur = cur.getAsJsonObject(p);
            }
        }
        var last = parts[parts.length - 1];
        cur.add(last, value);
    }

    /**
     * Converts a JSON element to its string representation.
     *
     * @param el the element
     * @return the string form, or an empty string if {@code el} is {@code null}
     */
    public static String jsonToString(JsonElement el) {
        if (el == null) return "";
        return el.isJsonPrimitive() ? el.getAsJsonPrimitive().getAsString() : String.valueOf(el);
    }

    /**
     * Parses a string into a typed {@link JsonPrimitive}, attempting
     * boolean, integer, and double parsing in that order.
     *
     * @param s the string to parse
     * @return a typed primitive
     */
    public static JsonPrimitive parsePrimitive(String s) {
        if (s == null) return new JsonPrimitive("");
        String str = s.trim();
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
            return new JsonPrimitive(Boolean.parseBoolean(str));
        }
        Maybe<JsonPrimitive> integer = str.matches("^-?\\d+$")
                ? Try.of(() -> Integer.parseInt(str)).toMaybe().map(JsonPrimitive::new)
                : Maybe.none();
        return integer.or(() -> str.matches("^-?\\d+(?:\\.\\d+)?$")
                        ? Try.of(() -> Double.parseDouble(str)).toMaybe().map(JsonPrimitive::new)
                        : Maybe.none())
                .orElseGet(() -> new JsonPrimitive(str));
    }

    /**
     * Casts a {@link ConfigUnit} to a raw {@code ConfigUnit<Object>} for
     * internal use.
     *
     * @param unit the unit to cast
     * @return the cast unit
     */
    @SuppressWarnings("unchecked")
    public static ConfigUnit<Object> castUnit(ConfigUnit<?> unit) {
        return (ConfigUnit<Object>) unit;
    }

    /**
     * Encodes a value to a {@link JsonObject} using its codec.
     *
     * @param codec the codec
     * @param value the value to encode
     * @return the encoded JSON object, or an empty object on failure
     */
    @SuppressWarnings("unchecked")
    public static JsonObject encodeToJsonObject(Codec<?> codec, Object value) {
        var res = ((Codec<Object>) codec).encodeStart(JsonOps.INSTANCE, value);
        var el = OptionalOps.toMaybe(res.result()).orElseGet(JsonObject::new);
        return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
    }

    /**
     * Converts a config value to JSON for use by the config screen.
     *
     * @param value  the value to encode
     * @param screen the config screen (provides the codec)
     * @return the encoded JSON object
     */
    public static JsonObject toJson(Object value, ConfigScreen screen) {
        var result = screen.codec.codec().encodeStart(JsonOps.INSTANCE, value);
        var encodeError = OptionalOps.toMaybe(result.error());
        if (encodeError.isDefined()) {
            OELibConfig.LOGGER.error("Failed to encode config {} to JSON: {}", screen.configId, encodeError.get().message());
            return new JsonObject();
        }
        var element = OptionalOps.toMaybe(result.result()).orElseGet(JsonObject::new);
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return new JsonObject();
    }

    /**
     * Creates a standard edit box widget for the config screen.
     *
     * @param initialValue the initial text
     * @param x            the x position
     * @param y            the y position
     * @param width        the width
     * @return a new edit box
     */
    public static EditBox createEditBox(String initialValue, int x, int y, int width) {
        EditBox box = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.empty());
        box.setMaxLength(2048);
        box.setValue(initialValue);
        box.setFocused(false);
        box.setCursorPosition(0);
        box.setHighlightPos(0);
        return box;
    }
}
