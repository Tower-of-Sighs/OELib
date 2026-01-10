package cc.sighs.oelib.config.util;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.serialization.TomlOps;
import cc.sighs.oelib.config.serialization.TomlTreeAdapter;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import de.marhali.json5.Json5;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class ConfigSerializationUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Json5 JSON5_PARSER = Json5.builder(builder -> builder.parseComments().build());
    private static final ConfigEncoder JSON_ENCODER = new ConfigEncoder() {
        @Override
        public <T> Optional<String> encode(T value, Codec<T> codec, List<ConfigValueMeta> fields, int version) {
            try {
                var result = codec.encodeStart(JsonOps.INSTANCE, value);
                if (result.error().isPresent()) {
                    logEncodeError(ConfigStorageFormat.JSON, result.error().get().message());
                    return Optional.empty();
                }
                var element = result.result().orElse(null);
                if (element == null) {
                    return Optional.empty();
                }
                if (version >= 0) {
                    element = addVersion(element, version);
                }
                var jsonStr = GSON.toJson(element);
                return Optional.of(jsonStr);
            } catch (Exception e) {
                OELib.LOGGER.error("Failed to encode config to JSON string", e);
                return Optional.empty();
            }
        }
    };
    private static final ConfigEncoder JSON5_ENCODER = new ConfigEncoder() {
        @Override
        public <T> Optional<String> encode(T value, Codec<T> codec, List<ConfigValueMeta> fields, int version) {
            try {
                var result = codec.encodeStart(JsonOps.INSTANCE, value);
                if (result.error().isPresent()) {
                    logEncodeError(ConfigStorageFormat.JSON5, result.error().get().message());
                    return Optional.empty();
                }
                var element = result.result().orElse(null);
                if (element == null) {
                    return Optional.empty();
                }
                if (version >= 0) {
                    element = addVersion(element, version);
                }
                var json5 = writeJson5WithComments(element, fields);
                return Optional.of(json5);
            } catch (Exception e) {
                OELib.LOGGER.error("Failed to encode config to JSON5 string", e);
                return Optional.empty();
            }
        }
    };
    private static final ConfigEncoder TOML_ENCODER = new ConfigEncoder() {
        @Override
        public <T> Optional<String> encode(T value, Codec<T> codec, List<ConfigValueMeta> fields, int version) {
            try {
                var result = codec.encodeStart(TomlOps.INSTANCE, value);
                if (result.error().isPresent()) {
                    logEncodeError(ConfigStorageFormat.TOML, result.error().get().message());
                    return Optional.empty();
                }
                var tree = result.result().orElse(null);
                if (tree == null) {
                    return Optional.empty();
                }
                var config = CommentedConfig.inMemory();
                TomlTreeAdapter.writeTree(config, tree, fields);
                if (version >= 0) {
                    config.set("__cfg_version", version);
                }
                StringWriter writer = new StringWriter();
                TomlFormat.instance().createWriter().write(config, writer);
                return Optional.of(writer.toString());
            } catch (Exception e) {
                OELib.LOGGER.error("Failed to encode config to TOML string", e);
                return Optional.empty();
            }
        }
    };

    private ConfigSerializationUtil() {
    }

    public static ConfigEncoder getEncoder(ConfigStorageFormat format) {
        return switch (format) {
            case JSON -> JSON_ENCODER;
            case JSON5 -> JSON5_ENCODER;
            case TOML -> TOML_ENCODER;
        };
    }

    public static <T> DataResult<T> parse(String payload, ConfigStorageFormat format, Codec<T> codec) {
        try {
            return switch (format) {
                case JSON -> {
                    var element = JsonParser.parseString(payload);
                    yield codec.parse(JsonOps.INSTANCE, element);
                }
                case JSON5 -> {
                    var json5Element = JSON5_PARSER.parse(payload);
                    var normalized = JSON5_PARSER.serialize(json5Element);
                    var element = JsonParser.parseString(normalized);
                    yield codec.parse(JsonOps.INSTANCE, element);
                }
                case TOML -> {
                    try (StringReader reader = new StringReader(payload)) {
                        var config = TomlFormat.instance().createParser().parse(reader);
                        var tree = TomlTreeAdapter.readTree(config);
                        yield codec.parse(TomlOps.INSTANCE, tree);
                    }
                }
            };
        } catch (Exception e) {
            OELib.LOGGER.error("Exception during parsing config with format {}", format, e);
            return DataResult.error(() -> "Parse exception: " + e.getMessage());
        }
    }

    public static Dynamic<?> parseToDynamic(String payload, ConfigStorageFormat format) {
        try {
            return switch (format) {
                case JSON -> {
                    var element = JsonParser.parseString(payload);
                    yield new Dynamic<>(JsonOps.INSTANCE, element);
                }
                case JSON5 -> {
                    var json5Element = JSON5_PARSER.parse(payload);
                    var normalized = JSON5_PARSER.serialize(json5Element);
                    var element = JsonParser.parseString(normalized);
                    yield new Dynamic<>(JsonOps.INSTANCE, element);
                }
                case TOML -> {
                    try (StringReader reader = new StringReader(payload)) {
                        var config = TomlFormat.instance().createParser().parse(reader);
                        var tree = TomlTreeAdapter.readTree(config);
                        yield new Dynamic<>(TomlOps.INSTANCE, tree);
                    }
                }
            };
        } catch (Exception e) {
            OELib.LOGGER.error("Exception during dynamic parse with format {}", format, e);
            return new Dynamic<>(JsonOps.INSTANCE, new JsonObject());
        }
    }

    public static <T> Optional<String> encodeToString(T value, ConfigStorageFormat format, Codec<T> codec, List<ConfigValueMeta> fields) {
        return getEncoder(format).encode(value, codec, fields, -1);
    }

    public static <T> Optional<String> encodeToStringWithVersion(T value, int version, ConfigStorageFormat format, Codec<T> codec, List<ConfigValueMeta> fields) {
        return getEncoder(format).encode(value, codec, fields, version);
    }

    private static JsonElement addVersion(JsonElement element, int version) {
        if (element.isJsonObject()) {
            element.getAsJsonObject().addProperty("__cfg_version", version);
            return element;
        }
        var obj = new JsonObject();
        obj.addProperty("__cfg_version", version);
        obj.add("value", element);
        return obj;
    }

    public static <T> Optional<T> loadFromFile(Path path, ConfigStorageFormat format, Codec<T> codec, T defaultValue) {
        try {
            if (!Files.exists(path)) {
                return Optional.of(defaultValue);
            }
            var content = Files.readString(path, StandardCharsets.UTF_8);
            var result = parse(content, format, codec);
            if (result.error().isPresent()) {
                OELib.LOGGER.error("Failed to load config from {}: {}", path, result.error().get().message());
                return Optional.of(defaultValue);
            }
            return result.result();
        } catch (Exception e) {
            OELib.LOGGER.error("Exception reading config file {}", path, e);
            return Optional.of(defaultValue);
        }
    }

    public static <T> boolean saveToFile(Path path, T value, ConfigStorageFormat format, Codec<T> codec, List<ConfigValueMeta> fields) {
        try {
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            var content = encodeToString(value, format, codec, fields);
            if (content.isEmpty()) {
                return false;
            }
            Files.writeString(path, content.get(), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to save config to {}", path, e);
            return false;
        }
    }

    private static String writeJson5WithComments(JsonElement element, List<ConfigValueMeta> fields) {
        StringBuilder sb = new StringBuilder();
        writeJson5Element(sb, element, fields, "", "");
        return sb.toString();
    }

    private static void writeJson5Element(StringBuilder sb, JsonElement element, List<ConfigValueMeta> fields, String indent, String path) {
        if (element.isJsonObject()) {
            var obj = element.getAsJsonObject();
            sb.append("{");
            if (!obj.entrySet().isEmpty()) {
                sb.append("\n");
                String childIndent = indent + "  ";
                int i = 0;
                int size = obj.entrySet().size();
                for (var entry : obj.entrySet()) {
                    String key = entry.getKey();
                    JsonElement value = entry.getValue();
                    String childPath = path.isEmpty() ? key : path + "." + key;
                    String comment = findComment(fields, childPath);
                    if (comment != null && !comment.isEmpty()) {
                        for (String line : comment.split("\n")) {
                            sb.append(childIndent).append("// ").append(line.trim()).append("\n");
                        }
                    }
                    sb.append(childIndent).append("\"").append(escapeJsonString(key)).append("\": ");
                    writeJson5Element(sb, value, fields, childIndent, childPath);
                    if (i < size - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                    i++;
                }
                sb.append(indent);
            }
            sb.append("}");
            return;
        }
        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            sb.append("[");
            if (!array.isEmpty()) {
                sb.append("\n");
                String childIndent = indent + "  ";
                int i = 0;
                int size = array.size();
                for (JsonElement value : array) {
                    sb.append(childIndent);
                    writeJson5Element(sb, value, fields, childIndent, path);
                    if (i < size - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                    i++;
                }
                sb.append(indent);
            }
            sb.append("]");
            return;
        }
        sb.append(element);
    }

    private static String findComment(List<ConfigValueMeta> fields, String path) {
        if (fields == null) {
            return null;
        }
        for (ConfigValueMeta meta : fields) {
            if (meta.key().equals(path) && meta.comment().isPresent()) {
                return meta.comment().get();
            }
        }
        return null;
    }

    private static String escapeJsonString(String input) {
        StringBuilder builder = new StringBuilder(input.length() + 8);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' || c == '"') {
                builder.append('\\').append(c);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static void logEncodeError(ConfigStorageFormat format, String message) {
        OELib.LOGGER.error("Failed to encode config to {}: {}", format, message);
    }

    public interface ConfigEncoder {
        <T> Optional<String> encode(T value, Codec<T> codec, List<ConfigValueMeta> fields, int version);
    }


}
