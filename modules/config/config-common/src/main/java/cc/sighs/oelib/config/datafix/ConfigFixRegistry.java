package cc.sighs.oelib.config.datafix;

import cc.sighs.oelib.config.OELibConfig;
import cc.sighs.oelib.config.util.ConfigPathUtil;
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

/**
 * Registry for versioned configuration datafix chains.
 *
 * <p>A datafix chain is a sequence of {@link Dynamic} transformations
 * keyed by source version. When a configuration file is loaded, its
 * {@code __cfg_version} field is compared against the chain's
 * {@linkplain Chain#currentVersion() current version}, and all
 * registered fixes from the file's version up to the current version
 * are applied in order.
 *
 * <p>Chains are registered via {@link #register(ResourceLocation, int, Consumer)}
 * before configurations are loaded.
 */
public final class ConfigFixRegistry {
    private static final Map<ResourceLocation, Chain> CHAINS = new LinkedHashMap<>();

    private ConfigFixRegistry() {
    }

    /**
     * Registers a datafix chain for the given configuration.
     *
     * @param id             the configuration ResourceLocation
     * @param currentVersion the latest datafix version
     * @param consumer       a consumer that populates the chain builder
     */
    public static void register(ResourceLocation id, int currentVersion, Consumer<Builder> consumer) {
        Builder b = new Builder(currentVersion);
        consumer.accept(b);
        CHAINS.put(id, b.build());
        OELibConfig.LOGGER.info("Registered config fix chain {} currentVersion={}", id, currentVersion);
    }

    /**
     * Returns the datafix chain for the given configuration, if registered.
     *
     * @param id the configuration ResourceLocation
     * @return the chain, or {@link Optional#empty()}
     */
    public static Optional<Chain> get(ResourceLocation id) {
        return Optional.ofNullable(CHAINS.get(id));
    }

    /**
     * Builder for a datafix chain.
     */
    public static final class Builder {
        private final int currentVersion;
        private final Map<Integer, UnaryOperator<Dynamic<?>>> fixes = new LinkedHashMap<>();

        public Builder(int currentVersion) {
            this.currentVersion = currentVersion;
        }

        /**
         * Registers a fix that upgrades from {@code fromVersion} to
         * {@code toVersion}.
         *
         * @param fromVersion the source version
         * @param toVersion   the target version (informational)
         * @param op          the transformation to apply
         * @return this builder
         */
        public Builder fix(int fromVersion, int toVersion, UnaryOperator<Dynamic<?>> op) {
            fixes.put(fromVersion, op);
            return this;
        }

        private Chain build() {
            return new Chain(currentVersion, fixes);
        }
    }

    /**
     * An ordered sequence of datafix transformations.
     */
    public static final class Chain {
        private final int currentVersion;
        private final Map<Integer, UnaryOperator<Dynamic<?>>> fixes;

        public Chain(int currentVersion, Map<Integer, UnaryOperator<Dynamic<?>>> fixes) {
            this.currentVersion = currentVersion;
            this.fixes = fixes;
        }

        /**
         * Returns the latest version in this chain.
         *
         * @return the current version
         */
        public int currentVersion() {
            return currentVersion;
        }

        /**
         * Applies all fixes from {@code inputVersion} up to
         * {@link #currentVersion} to the given dynamic.
         *
         * @param input        the dynamic to upgrade
         * @param inputVersion the version of the input
         * @return the upgraded dynamic
         */
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

    /**
     * Convenience context providing JSON-path helper methods for common
     * datafix operations such as renaming fields and setting defaults.
     */
    public static final class FixContext {
        private final Dynamic<?> dynamic;
        private final DynamicOps<?> targetOps;

        public FixContext(Dynamic<?> dynamic) {
            this.dynamic = dynamic;
            this.targetOps = dynamic.getOps();
        }

        /**
         * Parses a JSON string into a {@link JsonElement}.
         *
         * @param json the JSON string
         * @return the parsed element
         */
        public static JsonElement json(String json) {
            return JsonParser.parseString(json);
        }

        /**
         * Renames a field from {@code fromPath} to {@code toPath}.
         *
         * @param fromPath the source dotted path
         * @param toPath   the target dotted path
         * @return the updated dynamic
         */
        public Dynamic<?> rename(String fromPath, String toPath) {
            return modifyJson(obj -> {
                JsonElement value = ConfigPathUtil.getJsonByPath(obj, fromPath);
                if (value == null) {
                    return obj;
                }
                ConfigPathUtil.removeJsonByPath(obj, fromPath);
                ConfigPathUtil.setJsonByPath(obj, toPath, value);
                return obj;
            });
        }

        /**
         * Sets a field to the given value only if it is absent.
         *
         * @param path  the dotted path
         * @param value the value to set if missing
         * @return the updated dynamic
         */
        public Dynamic<?> setIfMissing(String path, JsonElement value) {
            return modifyJson(obj -> {
                JsonElement existing = ConfigPathUtil.getJsonByPath(obj, path);
                if (existing == null) {
                    ConfigPathUtil.setJsonByPath(obj, path, value);
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
