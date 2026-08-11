package cc.sighs.oelib.config.util;

import cc.sighs.oelib.config.OELibConfig;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import cc.sighs.oelib.config.serialization.TomlOps;
import cc.sighs.oelib.config.serialization.TomlTreeAdapter;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.flechazo.hkt.CheckedSupplier;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.util.OptionalOps;
import com.google.gson.*;
import com.mojang.serialization.*;
import de.marhali.json5.Json5;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 * Central serialization pipeline for encoding and decoding configuration
 * values across JSON, JSON5, and TOML formats.
 *
 * <p>Each format has a dedicated {@link ConfigEncoder} that handles the
 * specifics of encoding and comment injection. The parsing path uses
 * Mojang's {@link Codec} and {@link DynamicOps} abstractions.
 */
public final class ConfigSerializationUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Json5 JSON5_PARSER = Json5.builder(builder -> builder.parseComments().build());
    private static final ConfigEncoder JSON_ENCODER = new ConfigEncoder() {
        @Override
        public <T> Maybe<String> encode(T value, Codec<T> codec, List<ConfigValueMeta> fields, int version) {
            return encodeAttempt("JSON", () -> {
                var result = codec.encodeStart(JsonOps.INSTANCE, value);
                var error = OptionalOps.toMaybe(result.error());
                if (error.isDefined()) {
                    logEncodeError(ConfigStorageFormat.JSON, error.get().message());
                    return Maybe.none();
                }
                var encoded = OptionalOps.toMaybe(result.result());
                if (encoded.isEmpty()) {
                    return Maybe.none();
                }
                var element = encoded.get();
                if (version >= 0) {
                    element = addVersion(element, version);
                }
                var jsonStr = GSON.toJson(element);
                return Maybe.some(jsonStr);
            });
        }
    };
    private static final ConfigEncoder JSON5_ENCODER = new ConfigEncoder() {
        @Override
        public <T> Maybe<String> encode(T value, Codec<T> codec, List<ConfigValueMeta> fields, int version) {
            return encodeAttempt("JSON5", () -> {
                var result = codec.encodeStart(JsonOps.INSTANCE, value);
                var error = OptionalOps.toMaybe(result.error());
                if (error.isDefined()) {
                    logEncodeError(ConfigStorageFormat.JSON5, error.get().message());
                    return Maybe.none();
                }
                var encoded = OptionalOps.toMaybe(result.result());
                if (encoded.isEmpty()) {
                    return Maybe.none();
                }
                var element = encoded.get();
                if (version >= 0) {
                    element = addVersion(element, version);
                }
                var json5 = writeJson5WithComments(element, fields);
                return Maybe.some(json5);
            });
        }
    };
    private static final ConfigEncoder TOML_ENCODER = new ConfigEncoder() {
        @Override
        public <T> Maybe<String> encode(T value, Codec<T> codec, List<ConfigValueMeta> fields, int version) {
            return encodeAttempt("TOML", () -> {
                var result = codec.encodeStart(TomlOps.INSTANCE, value);
                var error = OptionalOps.toMaybe(result.error());
                if (error.isDefined()) {
                    logEncodeError(ConfigStorageFormat.TOML, error.get().message());
                    return Maybe.none();
                }
                var encoded = OptionalOps.toMaybe(result.result());
                if (encoded.isEmpty()) {
                    return Maybe.none();
                }
                var tree = encoded.get();
                var config = CommentedConfig.inMemory();
                TomlTreeAdapter.writeTree(config, tree, fields);
                if (version >= 0) {
                    config.set("__cfg_version", version);
                }
                StringWriter writer = new StringWriter();
                TomlFormat.instance().createWriter().write(config, writer);
                return Maybe.some(writer.toString());
            });
        }
    };

    private static <X extends Exception> Maybe<String> encodeAttempt(
            String format, CheckedSupplier<Maybe<String>, X> operation) {
        return Try.of(operation).fold(error -> {
            OELibConfig.LOGGER.error("Failed to encode config to {} string", format, error);
            return Maybe.none();
        }, encoded -> encoded);
    }

    private ConfigSerializationUtil() {
    }

    /**
     * Returns the encoder for the given storage format.
     *
     * @param format the format
     * @return the encoder
     */
    public static ConfigEncoder getEncoder(ConfigStorageFormat format) {
        return switch (format) {
            case JSON -> JSON_ENCODER;
            case JSON5 -> JSON5_ENCODER;
            case TOML -> TOML_ENCODER;
        };
    }

    /**
     * Parses a payload string into a value using the given codec.
     *
     * @param payload the encoded string
     * @param format  the format of the payload
     * @param codec   the codec to parse into
     * @param <T>     the target type
     * @return a {@link DataResult} containing the parsed value or an error
     */
    public static <T> DataResult<T> parse(String payload, ConfigStorageFormat format, Codec<T> codec) {
        return Try.of(() -> switch (format) {
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
                    var config = TomlFormat.instance().createParser().parse(new StringReader(payload));
                    var tree = TomlTreeAdapter.readTree(config);
                    yield codec.parse(TomlOps.INSTANCE, tree);
                }
            }).fold(error -> {
                OELibConfig.LOGGER.error(
                        "Exception during parsing config with format {}", format, error);
                return DataResult.error(() -> "Parse exception: " + error.getMessage());
            }, result -> result);
    }

    /**
     * Parses a payload string into a {@link Dynamic} without applying a codec.
     *
     * @param payload the encoded string
     * @param format  the format of the payload
     * @return a dynamic representing the parsed content
     * @throws IllegalArgumentException if the payload cannot be parsed in {@code format}
     */
    public static Dynamic<?> parseToDynamic(String payload, ConfigStorageFormat format) {
        return Try.of(() -> switch (format) {
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
                    var config = TomlFormat.instance().createParser().parse(new StringReader(payload));
                    var tree = TomlTreeAdapter.readTree(config);
                    yield new Dynamic<>(TomlOps.INSTANCE, tree);
                }
            }).fold(
                    error -> { throw new IllegalArgumentException(
                            "Failed to parse configuration as " + format + ": "
                                    + error.getMessage(), error); },
                    dynamic -> dynamic);
    }

    /**
     * Encodes a value to a string in the given format, without a version stamp.
     *
     * @param value  the value to encode
     * @param format the target format
     * @param codec  the codec for the value type
     * @param fields field metadata for comment injection
     * @param <T>    the value type
     * @return the encoded string, or an empty value on failure
     */
    public static <T> Maybe<String> encodeToString(T value, ConfigStorageFormat format, Codec<T> codec, List<ConfigValueMeta> fields) {
        return getEncoder(format).encode(value, codec, fields, -1);
    }

    /**
     * Encodes a value to a string with a {@code __cfg_version} stamp.
     *
     * @param value   the value to encode
     * @param version the version to stamp, or negative to omit
     * @param format  the target format
     * @param codec   the codec for the value type
     * @param fields  field metadata for comment injection
     * @param <T>     the value type
     * @return the encoded string, or an empty value on failure
     */
    public static <T> Maybe<String> encodeToStringWithVersion(T value, int version, ConfigStorageFormat format, Codec<T> codec, List<ConfigValueMeta> fields) {
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
            if (meta.key().equals(path) && meta.comment().isDefined()) {
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
        OELibConfig.LOGGER.error("Failed to encode config to {}: {}", format, message);
    }

    /**
     * Strategy interface for encoding a configuration value to a string.
     */
    public interface ConfigEncoder {
        /**
         * Encodes a configuration value to a string.
         *
         * @param value   the value to encode
         * @param codec   the codec for the value type
         * @param fields  field metadata for comment injection
         * @param version the datafix version to stamp, or negative to omit
         * @param <T>     the value type
         * @return the encoded string, or an empty value on failure
         */
        <T> Maybe<String> encode(T value, Codec<T> codec, List<ConfigValueMeta> fields, int version);
    }
}
