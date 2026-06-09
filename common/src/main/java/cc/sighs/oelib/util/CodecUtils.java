package cc.sighs.oelib.util;

import cc.sighs.oelib.OELib;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ApiStatus.Internal
public class CodecUtils {

    /**
     * Transforms the given {@link Codec} into a {@link MapCodec} by assuming that the result of all elements is a map.
     * <p>
     * This {@link MapCodec} will fail to encode or decode as long as the given {@link Codec} does not return or receive
     * a map.
     * <p>
     * from <a href="https://github.com/Mojang/DataFixerUpper/commit/a3542dd99f6374aeb81a94d7c06cfb3cc31f0155">DFU</a>
     */
    public static <A> MapCodec<A> assumeMapUnsafe(final Codec<A> codec) {
        return new MapCodec<>() {
            private static final String COMPRESSED_VALUE_KEY = "value";

            @Override
            public <T> Stream<T> keys(final DynamicOps<T> ops) {
                return Stream.of(ops.createString(COMPRESSED_VALUE_KEY));
            }

            @Override
            public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
                if (ops.compressMaps()) {
                    final T value = input.get(COMPRESSED_VALUE_KEY);
                    if (value == null) {
                        return DataResult.error(() -> "Missing value");
                    }
                    return codec.parse(ops, value);
                }
                return codec.parse(ops, ops.createMap(input.entries()));
            }

            @Override
            public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
                final DataResult<T> encoded = codec.encodeStart(ops, input);
                if (ops.compressMaps()) {
                    return prefix.add(COMPRESSED_VALUE_KEY, encoded);
                }
                final DataResult<MapLike<T>> encodedMapResult = encoded.flatMap(ops::getMap);
                return encodedMapResult.map(encodedMap -> {
                    encodedMap.entries().forEach(pair -> prefix.add(pair.getFirst(), pair.getSecond()));
                    return prefix;
                }).result().orElseGet(() -> prefix.withErrorsFrom(encodedMapResult));
            }
        };
    }

    private static final Map<Class<?>, Codec<?>> codecCache = new ConcurrentHashMap<>();

    public static <T> Optional<String> encodeToJson(Class<T> dataClass, Map<ResourceLocation, T> data) {
        return executeWithExceptionHandling(dataClass, "encoding", () -> {
            var mapCodec = createMapCodec(dataClass);
            var result = mapCodec.encodeStart(JsonOps.INSTANCE, data);

            return handleDataResult(dataClass, "encode", result)
                    .map(JsonElement::toString);
        });
    }

    public static <T> Optional<Map<ResourceLocation, T>> decodeFromJson(Class<T> dataClass, String jsonData) {
        return executeWithExceptionHandling(dataClass, "decoding", () -> {
            var jsonElement = JsonParser.parseString(jsonData);
            var mapCodec = createMapCodec(dataClass);
            var result = mapCodec.parse(JsonOps.INSTANCE, jsonElement);

            return handleDataResult(dataClass, "decode", result);
        });
    }

    public static <T> Optional<T> decodeSingle(Class<T> dataClass, JsonElement jsonElement) {
        return executeWithExceptionHandling(dataClass, "decoding", () -> {
            var codec = getCodec(dataClass);
            var result = codec.parse(JsonOps.INSTANCE, jsonElement);

            return handleDataResult(dataClass, "decode", result);
        });
    }

    private static <T, R> Optional<R> executeWithExceptionHandling(Class<T> dataClass, String operationType, Supplier<Optional<R>> action) {
        try {
            return action.get();
        } catch (Exception e) {
            OELib.LOGGER.error("Exception during {} {} for {}: {}",
                    dataClass.getSimpleName(), operationType, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static <T> Optional<T> handleDataResult(Class<?> dataClass, String operation, DataResult<T> result) {
        if (result.error().isPresent()) {
            var error = result.error().get();
            OELib.LOGGER.error("Failed to {} {} data: {}",
                    operation, dataClass.getSimpleName(), error.message());
            return Optional.empty();
        }
        return result.result();
    }

    private static <T> Codec<Map<ResourceLocation, T>> createMapCodec(Class<T> dataClass) {
        var codec = getCodec(dataClass);
        return Codec.unboundedMap(ResourceLocation.CODEC, codec);
    }


    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> Codec<T> getCodec(Class<T> dataClass) {
        return (Codec<T>) codecCache.computeIfAbsent(dataClass, cls -> {
            try {
                var codecField = cls.getDeclaredField("CODEC");
                codecField.setAccessible(true);
                return (Codec<?>) codecField.get(null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get CODEC field from " + cls.getSimpleName() +
                        ". Make sure the class has a public static final CODEC field.", e);
            }
        });
    }

    public static <T> void registerCodec(Class<T> dataClass, Codec<T> codec) {
        codecCache.put(dataClass, codec);
    }
}