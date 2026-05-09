package cc.sighs.oelib.network.serialization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.lang.invoke.MethodHandles;

/**
 * Internal utility for building {@link StreamCodec}s for Java records.
 *
 * <p>Call {@link #build(Class)} when you need a codec for a record type. The builder will try to
 * generate a fast codec (default), and falls back to the legacy reflection implementation if
 * generation is disabled or not possible for the given record.</p>
 *
 * <p>Note: the generated codec only inlines common leaf types (ints, strings, UUIDs, common MC
 * value types, etc.). Container types such as {@code Optional<T>} and {@code List<T>} are still
 * encoded/decoded via {@link ComponentIO}'s plan-based implementation. This is usually fine because
 * the hot-path win is mainly from avoiding reflection and avoiding generic dispatch for leaf
 * components.</p>
 */
public final class NetworkRecordCodecBuilder {

    private NetworkRecordCodecBuilder() {
    }

    /**
     * Builds a {@link StreamCodec} for the given record class by inspecting its components.
     *
     * @param recordClass the record class to analyze
     * @param <T>         the type of the record
     * @return a new StreamCodec for the record
     */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> build(Class<T> recordClass) {
        return build(MethodHandles.lookup(), recordClass);
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> build(MethodHandles.Lookup lookup, Class<T> recordClass) {
        StreamCodec<RegistryFriendlyByteBuf, T> generated = ClassFileCodecGenerator.tryGenerate(lookup, recordClass);
        if (generated != null) {
            return generated;
        }
        return NetworkRecordCodecBuilderLegacy.build(recordClass);
    }
}
