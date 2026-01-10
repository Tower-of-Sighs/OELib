package cc.sighs.oelib.config.datafix;

import cc.sighs.oelib.OELib;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class ConfigFixRegistry {
    private static final Map<ResourceLocation, Chain> CHAINS = new LinkedHashMap<>();

    private ConfigFixRegistry() {
    }

    public static void register(ResourceLocation id, int currentVersion, Consumer<Builder> consumer) {
        Builder b = new Builder(currentVersion);
        consumer.accept(b);
        CHAINS.put(id, b.build());
        OELib.LOGGER.info("Registered config fix chain {} currentVersion={}", id, currentVersion);
    }

    public static Optional<Chain> get(ResourceLocation id) {
        return Optional.ofNullable(CHAINS.get(id));
    }

    public static final class Builder {
        private final int currentVersion;
        private final Map<Integer, UnaryOperator<Dynamic<?>>> fixes = new LinkedHashMap<>();

        public Builder(int currentVersion) {
            this.currentVersion = currentVersion;
        }

        public Builder fix(int fromVersion, int toVersion, UnaryOperator<Dynamic<?>> op) {
            fixes.put(fromVersion, op);
            return this;
        }

        private Chain build() {
            return new Chain(currentVersion, fixes);
        }
    }

    public static final class Chain {
        private final int currentVersion;
        private final Map<Integer, UnaryOperator<Dynamic<?>>> fixes;

        public Chain(int currentVersion, Map<Integer, UnaryOperator<Dynamic<?>>> fixes) {
            this.currentVersion = currentVersion;
            this.fixes = fixes;
        }

        public int currentVersion() {
            return currentVersion;
        }

        public Dynamic<?> apply(Dynamic<?> input, int inputVersion) {
            Dynamic<?> cur = input;
            int v = inputVersion;
            while (v < currentVersion) {
                UnaryOperator<Dynamic<?>> f = fixes.get(v);
                if (f == null) {
                    break;
                }
                cur = f.apply(cur);
                v++;
            }
            return cur;
        }
    }

    public static final class FixContext {
        private final Dynamic<?> dynamic;
        private final DynamicOps<?> targetOps;

        public FixContext(Dynamic<?> dynamic) {
            this.dynamic = dynamic;
            this.targetOps = dynamic.getOps();
        }

        private static JsonElement getByPath(JsonObject obj, String path) {
            String[] parts = path.split("\\.");
            JsonObject cur = obj;
            for (int i = 0; i < parts.length; i++) {
                String key = parts[i];
                JsonElement e = cur.get(key);
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

        private static void putByPath(JsonObject obj, String path, JsonElement value) {
            String[] parts = path.split("\\.");
            JsonObject cur = obj;
            for (int i = 0; i < parts.length - 1; i++) {
                String key = parts[i];
                JsonElement e = cur.get(key);
                if (e == null || !e.isJsonObject()) {
                    JsonObject child = new JsonObject();
                    cur.add(key, child);
                    cur = child;
                } else {
                    cur = e.getAsJsonObject();
                }
            }
            cur.add(parts[parts.length - 1], value);
        }

        private static void removeByPath(JsonObject obj, String path) {
            String[] parts = path.split("\\.");
            JsonObject cur = obj;
            for (int i = 0; i < parts.length - 1; i++) {
                String key = parts[i];
                JsonElement e = cur.get(key);
                if (e == null || !e.isJsonObject()) {
                    return;
                }
                cur = e.getAsJsonObject();
            }
            cur.remove(parts[parts.length - 1]);
        }

        public static JsonElement json(String json) {
            return JsonParser.parseString(json);
        }

        public Dynamic<?> rename(String fromPath, String toPath) {
            return modifyJson(obj -> {
                JsonElement value = getByPath(obj, fromPath);
                if (value == null) {
                    return obj;
                }
                removeByPath(obj, fromPath);
                putByPath(obj, toPath, value);
                return obj;
            });
        }

        public Dynamic<?> setIfMissing(String path, JsonElement value) {
            return modifyJson(obj -> {
                JsonElement existing = getByPath(obj, path);
                if (existing == null) {
                    putByPath(obj, path, value);
                }
                return obj;
            });
        }

        private Dynamic<?> modifyJson(UnaryOperator<JsonObject> op) {
            Dynamic<JsonElement> json = dynamic.convert(JsonOps.INSTANCE);
            JsonElement root = json.getValue();
            if (!root.isJsonObject()) {
                return dynamic;
            }
            JsonObject obj = root.getAsJsonObject();
            JsonObject newObj = op.apply(obj);
            Dynamic<JsonElement> updatedJson = new Dynamic<>(JsonOps.INSTANCE, newObj);
            return updatedJson.convert(targetOps);
        }
    }
}
