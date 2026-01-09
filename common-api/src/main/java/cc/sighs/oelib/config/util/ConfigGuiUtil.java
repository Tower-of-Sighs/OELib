package cc.sighs.oelib.config.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
