package cc.sighs.oelib.config.util;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class ConfigGuiUtil {

    private ConfigGuiUtil() {
    }

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

    public static String jsonToString(JsonElement el) {
        if (el == null) return "";
        return el.isJsonPrimitive() ? el.getAsJsonPrimitive().getAsString() : String.valueOf(el);
    }

    public static JsonPrimitive parsePrimitive(String s) {
        if (s == null) return new JsonPrimitive("");
        String str = s.trim();
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
            return new JsonPrimitive(Boolean.parseBoolean(str));
        }
        if (str.matches("^-?\\d+$")) {
            try { return new JsonPrimitive(Integer.parseInt(str)); } catch (Exception ignored) {}
        }
        if (str.matches("^-?\\d+(?:\\.\\d+)?$")) {
            try { return new JsonPrimitive(Double.parseDouble(str)); } catch (Exception ignored) {}
        }
        return new JsonPrimitive(str);
    }

    @SuppressWarnings("unchecked")
    public static ConfigUnit<Object> castUnit(ConfigUnit<?> unit) {
        return (ConfigUnit<Object>) unit;
    }

    @SuppressWarnings("unchecked")
    public static JsonObject encodeToJsonObject(Codec<?> codec, Object value) {
        var res = ((Codec<Object>) codec).encodeStart(JsonOps.INSTANCE, value);
        var el = res.result().orElse(new JsonObject());
        return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
    }

    public static JsonObject toJson(Object value, ConfigScreen screen) {
        var result = screen.codec.codec().encodeStart(JsonOps.INSTANCE, value);
        if (result.error().isPresent()) {
            OELib.LOGGER.error("Failed to encode config {} to JSON: {}", screen.configId, result.error().get().message());
            return new JsonObject();
        }
        var element = result.result().orElse(new JsonObject());
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return new JsonObject();
    }

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
