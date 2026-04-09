package cc.sighs.oelib.network.serialization;

import io.netty.buffer.Unpooled;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Most-realistic encode/decode benchmarks that depend on a real Minecraft {@link RegistryAccess}.
 * <p>
 * This uses vanilla bootstrapping (BuiltInRegistries + Bootstrap) and exercises {@link RegistryCodec}
 * so that {@link RegistryFriendlyByteBuf#registryAccess()} is on the hot path.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class McRegistryRoundTripBenchmark {

    @Benchmark
    public void encode_legacy_newBuf(McState state, Blackhole blackhole) {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), state.registryAccess);
        try {
            state.legacy.encode(buf, state.packet);
            blackhole.consume(buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Benchmark
    public void encode_generated_newBuf(McState state, Blackhole blackhole) {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), state.registryAccess);
        try {
            state.generated.encode(buf, state.packet);
            blackhole.consume(buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Benchmark
    public void roundTrip_legacy_newBuf(McState state, Blackhole blackhole) {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), state.registryAccess);
        try {
            state.legacy.encode(buf, state.packet);
            buf.readerIndex(0);
            var decoded = state.legacy.decode(buf);
            blackhole.consume(decoded);
        } finally {
            buf.release();
        }
    }

    @Benchmark
    public void roundTrip_generated_newBuf(McState state, Blackhole blackhole) {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), state.registryAccess);
        try {
            state.generated.encode(buf, state.packet);
            buf.readerIndex(0);
            var decoded = state.generated.decode(buf);
            blackhole.consume(decoded);
        } finally {
            buf.release();
        }
    }

    /**
     * Packet type intentionally includes {@link RegistryCodec} so the codec interacts with registries.
     */
    public record RegistryItemPacket(
            int a,
            BlockPos pos,
            @RegistryCodec("minecraft:item")
            Holder<Item> item
    ) {
    }

    @State(Scope.Benchmark)
    public static class McState {
        private static final Object BOOTSTRAP_LOCK = new Object();
        private static volatile boolean BOOTSTRAPPED = false;

        RegistryAccess.Frozen registryAccess;
        RegistryItemPacket packet;

        StreamCodec<RegistryFriendlyByteBuf, RegistryItemPacket> legacy;
        StreamCodec<RegistryFriendlyByteBuf, RegistryItemPacket> generated;

        private static void ensureBootstrap() {
            if (BOOTSTRAPPED) return;
            synchronized (BOOTSTRAP_LOCK) {
                if (BOOTSTRAPPED) return;
                // Some bootstrap paths (e.g. DataFixers) require a version to be set,
                // otherwise SharedConstants.getCurrentVersion() throws "Game version not set".
                SharedConstants.tryDetectVersion();
                try {
                    SharedConstants.getCurrentVersion();
                } catch (IllegalStateException e) {
                    SharedConstants.setVersion(DetectedVersion.BUILT_IN);
                }
                Bootstrap.bootStrap();
                BOOTSTRAPPED = true;
            }
        }

        @Setup(Level.Trial)
        public void setup() {
            ensureBootstrap();

            registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

            var stoneId = Identifier.parse("minecraft:stone");
            Item stoneItem = BuiltInRegistries.ITEM.getOptional(stoneId)
                    .orElseThrow(() -> new IllegalStateException("Missing item: " + stoneId));
            Holder<Item> stone = BuiltInRegistries.ITEM.wrapAsHolder(stoneItem);
            packet = new RegistryItemPacket(123, new BlockPos(1, 64, 2), stone);

            legacy = NetworkRecordCodecBuilderLegacy.build(RegistryItemPacket.class);
            generated = NetworkRecordCodecBuilder.build(RegistryItemPacket.class);
        }
    }
}
