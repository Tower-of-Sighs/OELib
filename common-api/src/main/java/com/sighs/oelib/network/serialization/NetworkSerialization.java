package com.sighs.oelib.network.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.sighs.oelib.OELib;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.lang.reflect.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared helpers for network payload serialization.
 * <p>
 * Provides JSON based codecs as well as reflection based utilities that
 * simplify encoding and decoding of common payload types across platforms.
 * </p>
 */
public final class NetworkSerialization {

    private static final Map<Class<?>, StreamCodec<RegistryFriendlyByteBuf, ?>> RECORD_CODEC_CACHE = new ConcurrentHashMap<>();

    private NetworkSerialization() {
    }

    /**
     * Creates a JSON based {@link StreamCodec} using the given {@link Codec}.
     * <p>
     * Values are converted to JSON and carried as UTF-8 strings in the buffer.
     * </p>
     *
     * @param codec data codec
     * @param <T>   data type
     * @return stream codec usable for network payloads
     */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> jsonCodec(Codec<T> codec) {
        return new StreamCodec<>() {
            @Override
            public T decode(RegistryFriendlyByteBuf buf) {
                String json = buf.readUtf();
                JsonElement element = JsonParser.parseString(json);
                DataResult<T> result = codec.parse(JsonOps.INSTANCE, element);
                if (result.error().isPresent()) {
                    String message = result.error().get().message();
                    OELib.LOGGER.error("Failed to decode json payload: {}", message);
                    throw new IllegalStateException("Failed to decode json payload: " + message);
                }
                return result.result().orElseThrow(() ->
                        new IllegalStateException("Failed to decode json payload: empty result"));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, T value) {
                DataResult<JsonElement> result = codec.encodeStart(JsonOps.INSTANCE, value);
                if (result.error().isPresent()) {
                    String message = result.error().get().message();
                    OELib.LOGGER.error("Failed to encode json payload: {}", message);
                    throw new IllegalStateException("Failed to encode json payload: " + message);
                }
                JsonElement element = result.result().orElseThrow(() ->
                        new IllegalStateException("Failed to encode json payload: empty result"));
                buf.writeUtf(element.toString());
            }
        };
    }

    /**
     * Creates a {@link StreamCodec} for a Java record type using reflection.
     * <p>
     * The codec encodes all record components in declaration order using a
     * built-in mapping of Java types to buffer operations. Currently supported
     * component types are:
     * </p>
     * <ul>
     *     <li>{@code int}, {@code long}, {@code boolean}</li>
     *     <li>{@link String}</li>
     *     <li>{@link java.util.UUID}</li>
     *     <li>{@code byte[]}</li>
     * </ul>
     *
     * @param recordClass record type
     * @param <T>         record type
     * @return cached stream codec for the given record class
     */
    @SuppressWarnings("unchecked")
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> autoCodec(Class<T> recordClass) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("autoCodec only supports record types: " + recordClass.getName());
        }
        return (StreamCodec<RegistryFriendlyByteBuf, T>) RECORD_CODEC_CACHE.computeIfAbsent(recordClass, NetworkSerialization::buildRecordCodec);
    }

    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> buildRecordCodec(Class<T> recordClass) {
        RecordComponent[] components = recordClass.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Method[] accessors = new Method[components.length];
        @SuppressWarnings("unchecked")
        StreamCodec<RegistryFriendlyByteBuf, Object>[] customCodecs =
                (StreamCodec<RegistryFriendlyByteBuf, Object>[]) new StreamCodec<?, ?>[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            parameterTypes[i] = component.getType();
            accessors[i] = component.getAccessor();
            accessors[i].setAccessible(true);
            NetFieldCodec codecMeta = component.getAnnotation(NetFieldCodec.class);
            if (codecMeta != null) {
                try {
                    Field field = codecMeta.holder().getDeclaredField(codecMeta.field());
                    field.setAccessible(true);
                    Object codecObj = field.get(null);
                    if (!(codecObj instanceof StreamCodec<?, ?>)) {
                        throw new IllegalStateException("Field " + codecMeta.holder().getName() + "#"
                                + codecMeta.field() + " is not a StreamCodec");
                    }
                    @SuppressWarnings("unchecked")
                    StreamCodec<RegistryFriendlyByteBuf, Object> typedCodec =
                            (StreamCodec<RegistryFriendlyByteBuf, Object>) codecObj;
                    customCodecs[i] = typedCodec;
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new IllegalStateException("Failed to resolve codec field "
                            + codecMeta.holder().getName() + "#" + codecMeta.field()
                            + " for record " + recordClass.getName(), e);
                }
            }
        }
        Constructor<T> constructor;
        try {
            Constructor<T> ctor = recordClass.getDeclaredConstructor(parameterTypes);
            ctor.setAccessible(true);
            constructor = ctor;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to find canonical constructor for record " + recordClass.getName(), e);
        }

        return new StreamCodec<>() {
            @Override
            public T decode(RegistryFriendlyByteBuf buf) {
                Object[] values = new Object[components.length];
                for (int i = 0; i < components.length; i++) {
                    StreamCodec<RegistryFriendlyByteBuf, Object> codec = customCodecs[i];
                    if (codec != null) {
                        values[i] = codec.decode(buf);
                    } else {
                        Class<?> type = parameterTypes[i];
                        values[i] = readComponent(buf, type, components[i].getName(), recordClass);
                    }
                }
                try {
                    return constructor.newInstance(values);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Failed to construct record " + recordClass.getName(), e);
                }
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, T value) {
                for (int i = 0; i < components.length; i++) {
                    Object fieldValue;
                    try {
                        fieldValue = accessors[i].invoke(value);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new IllegalStateException("Failed to access component " + components[i].getName()
                                + " of record " + recordClass.getName(), e);
                    }
                    StreamCodec<RegistryFriendlyByteBuf, Object> codec = customCodecs[i];
                    if (codec != null) {
                        codec.encode(buf, fieldValue);
                    } else {
                        writeComponent(buf, parameterTypes[i], fieldValue, components[i].getName(), recordClass);
                    }
                }
            }
        };
    }

    private static Object readComponent(RegistryFriendlyByteBuf buf, Class<?> type, String name, Class<?> owner) {
        if (type == int.class || type == Integer.class) {
            return buf.readVarInt();
        }
        if (type == long.class || type == Long.class) {
            return buf.readVarLong();
        }
        if (type == boolean.class || type == Boolean.class) {
            return buf.readBoolean();
        }
        if (type == String.class) {
            return buf.readUtf();
        }
        if (type == UUID.class) {
            return buf.readUUID();
        }
        if (type == byte[].class) {
            return buf.readByteArray();
        }
        throw new IllegalStateException("Unsupported component type " + type.getName()
                + " for record " + owner.getName() + "#" + name);
    }

    private static void writeComponent(RegistryFriendlyByteBuf buf, Class<?> type, Object value, String name, Class<?> owner) {
        if (type == int.class || type == Integer.class) {
            buf.writeVarInt((Integer) value);
            return;
        }
        if (type == long.class || type == Long.class) {
            buf.writeVarLong((Long) value);
            return;
        }
        if (type == boolean.class || type == Boolean.class) {
            buf.writeBoolean((Boolean) value);
            return;
        }
        if (type == String.class) {
            buf.writeUtf((String) value);
            return;
        }
        if (type == UUID.class) {
            buf.writeUUID((UUID) value);
            return;
        }
        if (type == byte[].class) {
            buf.writeByteArray((byte[]) value);
            return;
        }
        throw new IllegalStateException("Unsupported component type " + type.getName()
                + " for record " + owner.getName() + "#" + name);
    }
}
