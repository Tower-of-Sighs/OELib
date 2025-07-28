package com.mafuyu404.oelib.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mafuyu404.oelib.OElib;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 编解码工具类。
 * <p>
 * 提供通用的数据序列化和反序列化功能。
 * </p>
 *
 * @author Flechazo
 * @since 1.0.0
 */
public class CodecUtils {

    private static final Map<Class<?>, Codec<?>> codecCache = new ConcurrentHashMap<>();

    public static <T> Optional<String> encodeToJson(Class<T> dataClass, Map<ResourceLocation, T> data) {
        try {
            Codec<Map<ResourceLocation, T>> mapCodec = createMapCodec(dataClass);
            DataResult<JsonElement> result = mapCodec.encodeStart(JsonOps.INSTANCE, data);

            if (result.error().isPresent()) {
                var error = result.error().get();
                OElib.LOGGER.error("Failed to encode {} data: {}", dataClass.getSimpleName(), error.message());
                return Optional.empty();
            }

            return result.result().map(JsonElement::toString);
        } catch (Exception e) {
            OElib.LOGGER.error("Exception during {} encoding: {}", dataClass.getSimpleName(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    public static <T> Optional<Map<ResourceLocation, T>> decodeFromJson(Class<T> dataClass, String jsonData) {
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonData);
            Codec<Map<ResourceLocation, T>> mapCodec = createMapCodec(dataClass);
            DataResult<Map<ResourceLocation, T>> result = mapCodec.parse(JsonOps.INSTANCE, jsonElement);

            if (result.error().isPresent()) {
                var error = result.error().get();
                OElib.LOGGER.error("Failed to decode {} data: {}", dataClass.getSimpleName(), error.message());
                return Optional.empty();
            }

            return result.result();
        } catch (Exception e) {
            OElib.LOGGER.error("Exception during {} decoding: {}", dataClass.getSimpleName(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    public static <T> Optional<T> decodeSingle(Class<T> dataClass, JsonElement jsonElement) {
        try {
            Codec<T> codec = getCodec(dataClass);
            DataResult<T> result = codec.parse(JsonOps.INSTANCE, jsonElement);

            if (result.error().isPresent()) {
                var error = result.error().get();
                OElib.LOGGER.error("Failed to decode {} data: {}", dataClass.getSimpleName(), error.message());
                return Optional.empty();
            }

            return result.result();
        } catch (Exception e) {
            OElib.LOGGER.error("Exception during {} decoding: {}", dataClass.getSimpleName(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static <T> Codec<Map<ResourceLocation, T>> createMapCodec(Class<T> dataClass) {
        Codec<T> codec = getCodec(dataClass);
        return Codec.unboundedMap(ResourceLocation.CODEC, codec);
    }

    @SuppressWarnings("unchecked")
    private static <T> Codec<T> getCodec(Class<T> dataClass) {
        return (Codec<T>) codecCache.computeIfAbsent(dataClass, cls -> {
            try {
                Field codecField = cls.getDeclaredField("CODEC");
                codecField.setAccessible(true);
                return (Codec<?>) codecField.get(null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get CODEC field from " + cls.getSimpleName() +
                        ". Make sure the class has a public static final CODEC field.", e);
            }
        });
    }
}