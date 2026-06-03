package cc.sighs.oelib.config.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

/**
 * Gson-related utilities for cleaning up JSON objects.
 */
public final class GsonUtil {

    private GsonUtil() {
    }

    /**
     * Removes all entries whose value is a blank string from the given
     * JSON object.
     *
     * @param obj the JSON object to clean, or {@code null} for no-op
     */
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

    /**
     * Removes all blank-string elements from the given JSON array.
     *
     * @param arr the JSON array to clean, or {@code null} for no-op
     */
    public static void removeBlankStringElements(JsonArray arr) {
        if (arr == null) return;
        for (int i = arr.size() - 1; i >= 0; i--) {
            var el = arr.get(i);
            if (isBlankString(el)) {
                arr.remove(i);
            }
        }
    }

    /**
     * Returns {@code true} if the given element is a JSON primitive string
     * that is blank.
     *
     * @param el the element to test
     * @return {@code true} if the element is a blank string
     */
    public static boolean isBlankString(JsonElement el) {
        return el != null
                && el.isJsonPrimitive()
                && el.getAsJsonPrimitive().isString()
                && el.getAsString().isBlank();
    }
}
