package com.mafuyu404.oelib.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mafuyu404.oelib.OELib;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 编解码工具类。
 * <p>
 * 提供通用的针对数据包序列化和反序列化功能。
 * </p>
 */
public class CodecUtils {

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