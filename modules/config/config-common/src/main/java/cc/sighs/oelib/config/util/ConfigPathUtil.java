package cc.sighs.oelib.config.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Path-based access helpers for serialized JSON trees.
 *
 * <p>Methods in this class navigate dotted paths (for example
 * {@code "db.host"}) into JSON objects. Runtime objects use schema-compiled optics.
 */
public final class ConfigPathUtil {
    private ConfigPathUtil() {
    }

    /**
     * Retrieves the JSON element at the given dotted path within a JSON object.
     *
     * @param obj  the root JSON object
     * @param path the dotted path
     * @return the element at the path, or {@code null} if the path does not exist
     */
    public static JsonElement getJsonByPath(JsonObject obj, String path) {
        String[] parts = path.split("\\.");
        JsonObject cur = obj;
        for (int i = 0; i < parts.length; i++) {
            JsonElement e = cur.get(parts[i]);
            if (e == null) {
                return null;
            }
            if (i == parts.length - 1) {
                return e;
            }
            if (!e.isJsonObject()) {
                return null;
            }
            cur = e.getAsJsonObject();
        }
        return null;
    }

    /**
     * Sets the JSON element at the given dotted path, creating intermediate
     * objects as needed.
     *
     * @param obj   the root JSON object (mutated in place)
     * @param path  the dotted path
     * @param value the value to set
     */
    public static void setJsonByPath(JsonObject obj, String path, JsonElement value) {
        String[] parts = path.split("\\.");
        JsonObject cur = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement e = cur.get(parts[i]);
            if (e == null || !e.isJsonObject()) {
                JsonObject next = new JsonObject();
                cur.add(parts[i], next);
                cur = next;
            } else {
                cur = e.getAsJsonObject();
            }
        }
        cur.add(parts[parts.length - 1], value);
    }

    /**
     * Removes the JSON element at the given dotted path.
     *
     * @param obj  the root JSON object (mutated in place)
     * @param path the dotted path
     */
    public static void removeJsonByPath(JsonObject obj, String path) {
        String[] parts = path.split("\\.");
        JsonObject cur = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement e = cur.get(parts[i]);
            if (e == null || !e.isJsonObject()) {
                return;
            }
            cur = e.getAsJsonObject();
        }
        cur.remove(parts[parts.length - 1]);
    }

}
