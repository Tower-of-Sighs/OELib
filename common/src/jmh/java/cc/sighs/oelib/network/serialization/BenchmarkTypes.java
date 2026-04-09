package cc.sighs.oelib.network.serialization;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared JMH benchmark fixtures.
 * <p>
 * Intentionally kept inside the same package as the serialization implementation so we can
 * benchmark package-private paths (e.g. {@link CustomCodecResolver}, {@link ComponentIO}).
 */
final class BenchmarkTypes {

    static final Type OPTIONAL_LIST_INT = genericTypeOf(TypeTokens.class, "optionalListInt");

    // -----------------------------
    // Records under test
    // -----------------------------
    static final Type LIST_UUID = genericTypeOf(TypeTokens.class, "listUuid");
    static final Type OPTIONAL_STRING = genericTypeOf(TypeTokens.class, "optionalString");

    // -----------------------------
    // Holder classes used by annotations
    // -----------------------------

    private BenchmarkTypes() {
    }

    private static Type genericTypeOf(Class<?> holder, String fieldName) {
        try {
            Field f = holder.getDeclaredField(fieldName);
            return f.getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    // -----------------------------
    // Generic types for planOf benchmarks
    // -----------------------------

    record PlainPacket(
            int a,
            long b,
            String s,
            UUID id,
            Optional<List<Integer>> ints
    ) {
    }

    record AnnotatedPacket(
            @NetFieldCodec(holder = NetCodecs.class)
            String netDirect,

            // Empty field name forces the resolver into the semantic-scan path without logging warnings.
            @NetFieldCodec(holder = NetCodecs.class, field = "")
            Optional<String> netScanned,

            @JsonCodec(holder = JsonCodecs.class)
            String jsonDirect,

            // Empty field name forces semantic-scan path.
            @JsonCodec(holder = JsonCodecs.class, field = "")
            String jsonScanned,

            @RegistryCodec("minecraft:item")
            Holder<Identifier> registryValue
    ) {
    }

    static final class NetCodecs {
        public static final StreamCodec<RegistryFriendlyByteBuf, String> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public String decode(RegistryFriendlyByteBuf buf) {
                return buf.readUtf();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, String value) {
                buf.writeUtf(value);
            }
        };
        public static final StreamCodec<RegistryFriendlyByteBuf, Optional<String>> OPTIONAL_STRING_CODEC = new StreamCodec<>() {
            @Override
            public Optional<String> decode(RegistryFriendlyByteBuf buf) {
                return buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, Optional<String> value) {
                boolean present = value != null && value.isPresent();
                buf.writeBoolean(present);
                if (present) {
                    buf.writeUtf(value.get());
                }
            }
        };
        public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Integer>> OPTIONAL_INT_CODEC = new StreamCodec<>() {
            @Override
            public Optional<Integer> decode(RegistryFriendlyByteBuf buf) {
                return buf.readBoolean() ? Optional.of(buf.readVarInt()) : Optional.empty();
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, Optional<Integer> value) {
                boolean present = value != null && value.isPresent();
                buf.writeBoolean(present);
                if (present) {
                    buf.writeVarInt(value.get());
                }
            }
        };
        // Deliberately "worse" candidate: ByteBuf instead of RegistryFriendlyByteBuf (penalized by resolver).
        public static final StreamCodec<ByteBuf, Optional<String>> OPTIONAL_STRING_BYTEBUF_CODEC = new StreamCodec<>() {
            @Override
            public Optional<String> decode(ByteBuf buf) {
                return Optional.empty();
            }

            @Override
            public void encode(ByteBuf buf, Optional<String> value) {
            }
        };

        private NetCodecs() {
        }
    }

    static final class JsonCodecs {
        public static final Codec<String> CODEC = Codec.STRING;
        public static final Codec<Integer> INT_CODEC = Codec.INT;
        public static final Codec<String> ALT_CODEC = Codec.STRING;

        private JsonCodecs() {
        }
    }

    @SuppressWarnings("unused")
    private static final class TypeTokens {
        Optional<List<Integer>> optionalListInt;
        List<UUID> listUuid;
        Optional<String> optionalString;
    }
}

