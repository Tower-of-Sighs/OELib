package cc.sighs.oelib.network.serialization;

import cc.sighs.oelib.OELib;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
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

    /**
     * Cache for generated record codecs to avoid repetitive reflection overhead.
     */
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
            @NotNull
            public T decode(RegistryFriendlyByteBuf buf) {
                var json = buf.readUtf();
                var element = JsonParser.parseString(json);
                var result = codec.parse(JsonOps.INSTANCE, element);
                if (result.error().isPresent()) {
                    var message = result.error().get().message();
                    OELib.LOGGER.error("Failed to decode json payload: {}", message);
                    throw new IllegalStateException("Failed to decode json payload: " + message);
                }
                return result.result().orElseThrow(() ->
                        new IllegalStateException("Failed to decode json payload: empty result"));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, T value) {
                var result = codec.encodeStart(JsonOps.INSTANCE, value);
                if (result.error().isPresent()) {
                    var message = result.error().get().message();
                    OELib.LOGGER.error("Failed to encode json payload: {}", message);
                    throw new IllegalStateException("Failed to encode json payload: " + message);
                }
                var element = result.result().orElseThrow(() ->
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
     * <li>{@code int}, {@code long}, {@code boolean}</li>
     * <li>{@link String}</li>
     * <li>{@link java.util.UUID}</li>
     * <li>{@code byte[]}</li>
     * <li>{@link Enum}</li>
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

    /**
     * Internal logic to build a {@link StreamCodec} for a record class by analyzing its components.
     *
     * @param recordClass the record class to analyze
     * @param <T>         the type of the record
     * @return a new StreamCodec for the record
     * @throws IllegalStateException if the canonical constructor or custom codecs cannot be resolved
     */
    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> buildRecordCodec(Class<T> recordClass) {
        var components = recordClass.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Method[] accessors = new Method[components.length];
        @SuppressWarnings("unchecked")
        StreamCodec<RegistryFriendlyByteBuf, Object>[] customCodecs =
                (StreamCodec<RegistryFriendlyByteBuf, Object>[]) new StreamCodec<?, ?>[components.length];
        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            parameterTypes[i] = component.getType();
            accessors[i] = component.getAccessor();
            accessors[i].setAccessible(true);

            customCodecs[i] = resolveCustomCodec(recordClass, component);
        }
        Constructor<T> constructor;
        try {
            var ctor = recordClass.getDeclaredConstructor(parameterTypes);
            ctor.setAccessible(true);
            constructor = ctor;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to find canonical constructor for record " + recordClass.getName(), e);
        }

        return new StreamCodec<>() {
            @Override
            @NotNull
            public T decode(RegistryFriendlyByteBuf buf) {
                Object[] values = new Object[components.length];
                for (int i = 0; i < components.length; i++) {
                    var codec = customCodecs[i];
                    if (codec != null) {
                        values[i] = codec.decode(buf);
                    } else {
                        var type = parameterTypes[i];
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
                    var codec = customCodecs[i];
                    if (codec != null) {
                        codec.encode(buf, fieldValue);
                    } else {
                        writeComponent(buf, parameterTypes[i], fieldValue, components[i].getName(), recordClass);
                    }
                }
            }
        };
    }

    /**
     * Resolves a custom codec from a {@link NetFieldCodec} annotation if present on a record component.
     *
     * @param recordClass the record class containing the component
     * @param component   the record component to check for annotations
     * @return the resolved StreamCodec, or null if no annotation is present
     * @throws IllegalStateException if the field specified in the annotation is invalid or inaccessible
     */
    @SuppressWarnings("unchecked")
    private static StreamCodec<RegistryFriendlyByteBuf, Object> resolveCustomCodec(Class<?> recordClass, RecordComponent component) {
        var codecMeta = component.getAnnotation(NetFieldCodec.class);
        if (codecMeta == null) {
            return null;
        }

        try {
            var field = codecMeta.holder().getDeclaredField(codecMeta.field());
            field.setAccessible(true);
            var codecObj = field.get(null);

            if (!(codecObj instanceof StreamCodec<?, ?>)) {
                throw new IllegalStateException("Field " + codecMeta.holder().getName() + "#"
                        + codecMeta.field() + " is not a StreamCodec");
            }

            return (StreamCodec<RegistryFriendlyByteBuf, Object>) codecObj;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to resolve codec field "
                    + codecMeta.holder().getName() + "#" + codecMeta.field()
                    + " for record " + recordClass.getName(), e);
        }
    }

    /**
     * Dispatches the buffer read operation based on the component type.
     *
     * @param buf   the buffer to read from
     * @param type  the class type of the component
     * @param name  the name of the component (for error reporting)
     * @param owner the record class owning the component (for error reporting)
     * @return the decoded object
     * @throws IllegalStateException if the type is not supported
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
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
        if (type.isEnum()) {
            return buf.readEnum((Class<? extends Enum>) type);
        }
        throw new IllegalStateException("Unsupported component type " + type.getName()
                + " for record " + owner.getName() + "#" + name);
    }

    /**
     * Dispatches the buffer write operation based on the component type.
     *
     * @param buf   the buffer to write to
     * @param type  the class type of the component
     * @param value the value to encode
     * @param name  the name of the component (for error reporting)
     * @param owner the record class owning the component (for error reporting)
     * @throws IllegalStateException if the type is not supported
     */
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
        if (type.isEnum()) {
            buf.writeEnum((Enum<?>) value);
            return;
        }
        throw new IllegalStateException("Unsupported component type " + type.getName()
                + " for record " + owner.getName() + "#" + name);
    }
}