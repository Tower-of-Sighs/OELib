package cc.sighs.oelib.config.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

public final class GsonUtil {

    private GsonUtil() {
    }

    public static void removeBlankStringValues(JsonObject obj) {
        if (obj == null) return;
        var entries = new ArrayList<>(obj.entrySet());
        for (var e : entries) {
            var v = e.getValue();
            if (isBlankString(v)) {
                obj.remove(e.getKey());
            }
        }
    }

    public static void removeBlankStringElements(JsonArray arr) {
        if (arr == null) return;
        for (int i = arr.size() - 1; i >= 0; i--) {
            var el = arr.get(i);
            if (isBlankString(el)) {
                arr.remove(i);
            }
        }
    }

    public static boolean isBlankString(JsonElement el) {
        return el != null
                && el.isJsonPrimitive()
                && el.getAsJsonPrimitive().isString()
                && el.getAsString().isBlank();
    }
}