package cc.sighs.oelib.config.util;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Applies per-field datafix migrations to a configuration value at load time.
 */
public final class ConfigMigrationUtil {
    private ConfigMigrationUtil() {
    }

    /**
     * Collects all field-level migrations from the given metadata list,
     * sorts them by version, and applies them to the corresponding fields
     * in the configuration value.
     *
     * <p>If no migrations are registered, the original value is returned
     * unchanged.
     *
     * @param value  the loaded configuration value
     * @param codec  the codec for encoding/decoding the value to JSON
     * @param fields the field metadata containing migrations
     * @param <T>    the configuration value type
     * @return the migrated value, or the original if no migrations apply
     */
    public static <T> T applyFieldMigrations(T value, Codec<T> codec, List<ConfigValueMeta> fields) {
        List<FieldMigrationEntry> entries = collect(entriesFrom(fields));
        if (entries.isEmpty()) {
            return value;
        }

        var encoded = codec.encodeStart(JsonOps.INSTANCE, value);
        JsonElement rootElement = encoded.result().orElse(null);
        if (rootElement == null || !rootElement.isJsonObject()) {
            return value;
        }

        JsonObject root = rootElement.getAsJsonObject();
        for (FieldMigrationEntry entry : entries) {
            JsonElement fieldElement = ConfigPathUtil.getJsonByPath(root, entry.path());
            if (fieldElement == null) {
                continue;
            }
            Dynamic<JsonElement> fieldDynamic = new Dynamic<>(JsonOps.INSTANCE, fieldElement);
            Dynamic<?> migrated = entry.migration().apply(fieldDynamic);
            JsonElement migratedElement = migrated.convert(JsonOps.INSTANCE).getValue();
            ConfigPathUtil.setJsonByPath(root, entry.path(), migratedElement);
        }

        var parsed = codec.parse(JsonOps.INSTANCE, root);
        return parsed.result().orElse(value);
    }

    private static List<FieldMigrationEntry> collect(List<FieldMigrationEntry> entries) {
        entries.sort(Comparator.comparingInt(FieldMigrationEntry::version));
        return entries;
    }

    private static List<FieldMigrationEntry> entriesFrom(List<ConfigValueMeta> fields) {
        List<FieldMigrationEntry> entries = new ArrayList<>();
        for (ConfigValueMeta fieldMeta : fields) {
            for (ConfigValueMeta.FieldMigration migration : fieldMeta.migrations()) {
                entries.add(new FieldMigrationEntry(fieldMeta.key(), migration.version(), migration.migration()));
            }
        }
        return entries;
    }

    private record FieldMigrationEntry(String path, int version, UnaryOperator<Dynamic<?>> migration) {
    }
}
