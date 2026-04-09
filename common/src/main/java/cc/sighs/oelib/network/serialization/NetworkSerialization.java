package cc.sighs.oelib.network.serialization;

import cc.sighs.oelib.OELib;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared helpers for network payload serialization.
 *
 * <p>This class provides:</p>
 * <ul>
 *     <li>{@link #jsonCodec(Codec)}: encode values as JSON strings</li>
 *     <li>{@link #autoCodec(Class)}: build and cache a {@link StreamCodec} for a record packet</li>
 * </ul>
 */
public final class NetworkSerialization {

    private static final ConcurrentHashMap<Class<?>, StreamCodec<RegistryFriendlyByteBuf, ?>> RECORD_CODEC_CACHE = new ConcurrentHashMap<>();

    private NetworkSerialization() {
    }

    /**
     * Creates a JSON-based {@link StreamCodec} using the given {@link Codec}.
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
     * Returns a cached {@link StreamCodec} for a Java record type.
     *
     * <p>The codec encodes all record components in declaration order.
     * Custom codecs can be provided via annotations such as {@link NetFieldCodec},
     * {@link JsonCodec} and {@link RegistryCodec}.</p>
     *
     * @param recordClass record type (must be a record)
     * @param <T>         record type
     * @return cached stream codec for the given record class
     */
    @SuppressWarnings("unchecked")
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> autoCodec(Class<T> recordClass) {
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("autoCodec only supports record types: " + recordClass.getName());
        }
        return (StreamCodec<RegistryFriendlyByteBuf, T>) RECORD_CODEC_CACHE.computeIfAbsent(
                recordClass,
                cls -> NetworkRecordCodecBuilder.build((Class) cls)
        );
    }
}
