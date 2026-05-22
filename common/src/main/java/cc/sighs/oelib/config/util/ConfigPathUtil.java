package cc.sighs.oelib.config.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Path-based access helpers for JSON trees and in-memory record graphs.
 *
 * <p>Methods in this class navigate dotted paths (for example
 * {@code "db.host"}) into JSON objects or Java record hierarchies.
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

    /**
     * Navigates a dotted path into a Java object graph, traversing record
     * accessors and map entries.
     *
     * <p>At each segment, if the current object is a {@link Map}, the
     * segment is used as a map key. If it is a record, the segment is used
     * as a component accessor method name.
     *
     * @param root the root object
     * @param path the dotted path
     * @return the object at the path, or {@code null} if the path cannot be resolved
     */
    public static Object getObjectByPath(Object root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        Object current = root;
        String[] parts = path.split("\\.");
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
                continue;
            }
            Class<?> cls = current.getClass();
            if (!cls.isRecord()) {
                return null;
            }
            try {
                var accessor = cls.getDeclaredMethod(part);
                if (!accessor.canAccess(current)) {
                    accessor.setAccessible(true);
                }
                current = accessor.invoke(current);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                return null;
            }
        }
        return current;
    }
}
