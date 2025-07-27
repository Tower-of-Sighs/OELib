package com.mafuyu404.oelib.util;

import com.google.gson.JsonElement;
import com.mafuyu404.oelib.OElib;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

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
    
    /**
     * 将数据序列化为 JSON 字符串。
     *
     * @param dataClass 数据类型
     * @param data 数据
     * @param <T> 数据类型泛型
     * @return JSON 字符串，失败时返回 null
     */
    public static <T> String encodeToJson(Class<T> dataClass, Map<ResourceLocation, T> data) {
        try {
            Codec<Map<ResourceLocation, T>> mapCodec = createMapCodec(dataClass);
            DataResult<JsonElement> result = mapCodec.encodeStart(JsonOps.INSTANCE, data);
            
            if (result.error().isPresent()) {
                OElib.LOGGER.error("Failed to encode {} data: {}", dataClass.getSimpleName(), result.error().get().message());
                return null;
            }
            
            return result.result().orElse(null).toString();
        } catch (Exception e) {
            OElib.LOGGER.error("Exception during {} encoding: {}", dataClass.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从 JSON 字符串反序列化数据。
     *
     * @param dataClass 数据类型
     * @param jsonData JSON 字符串
     * @param <T> 数据类型泛型
     * @return 数据映射，失败时返回 null
     */
    public static <T> Map<ResourceLocation, T> decodeFromJson(Class<T> dataClass, String jsonData) {
        try {
            JsonElement jsonElement = com.google.gson.JsonParser.parseString(jsonData);
            Codec<Map<ResourceLocation, T>> mapCodec = createMapCodec(dataClass);
            DataResult<Map<ResourceLocation, T>> result = mapCodec.parse(JsonOps.INSTANCE, jsonElement);
            
            if (result.error().isPresent()) {
                OElib.LOGGER.error("Failed to decode {} data: {}", dataClass.getSimpleName(), result.error().get().message());
                return null;
            }
            
            return result.result().orElse(null);
        } catch (Exception e) {
            OElib.LOGGER.error("Exception during {} decoding: {}", dataClass.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从 JsonElement 反序列化单个数据对象。
     *
     * @param dataClass 数据类型
     * @param jsonElement JSON 元素
     * @param <T> 数据类型泛型
     * @return 数据对象，失败时返回 null
     */
    public static <T> T decodeSingle(Class<T> dataClass, JsonElement jsonElement) {
        try {
            Codec<T> codec = getCodec(dataClass);
            DataResult<T> result = codec.parse(JsonOps.INSTANCE, jsonElement);
            
            if (result.error().isPresent()) {
                OElib.LOGGER.error("Failed to decode {} data: {}", dataClass.getSimpleName(), result.error().get().message());
                return null;
            }
            
            return result.result().orElse(null);
        } catch (Exception e) {
            OElib.LOGGER.error("Exception during {} decoding: {}", dataClass.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    private static <T> Codec<Map<ResourceLocation, T>> createMapCodec(Class<T> dataClass) {
        Codec<T> codec = getCodec(dataClass);
        return Codec.unboundedMap(ResourceLocation.CODEC, codec);
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Codec<T> getCodec(Class<T> dataClass) {
        try {
            Field codecField = dataClass.getDeclaredField("CODEC");
            codecField.setAccessible(true);
            return (Codec<T>) codecField.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get CODEC field from " + dataClass.getSimpleName() + 
                    ". Make sure the class has a public static final CODEC field.", e);
        }
    }
}