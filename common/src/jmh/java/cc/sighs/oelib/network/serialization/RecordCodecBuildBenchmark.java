package cc.sighs.oelib.network.serialization;

import net.minecraft.network.codec.StreamCodec;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for the reflection-driven record codec builder.
 * <p>
 * This is the "before" baseline. After migrating to Java 25 Class-File API,
 * rerun the same benchmarks on the updated implementation to compare.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class RecordCodecBuildBenchmark {

    @Benchmark
    public void build_plainPacket_legacy(Blackhole blackhole) {
        StreamCodec<?, ?> codec = NetworkRecordCodecBuilderLegacy.build(BenchmarkTypes.PlainPacket.class);
        blackhole.consume(codec);
    }

    @Benchmark
    public void build_plainPacket_generated(Blackhole blackhole) {
        StreamCodec<?, ?> codec = NetworkRecordCodecBuilder.build(BenchmarkTypes.PlainPacket.class);
        blackhole.consume(codec);
    }

    @Benchmark
    public void build_annotatedPacket_legacy(Blackhole blackhole) {
        StreamCodec<?, ?> codec = NetworkRecordCodecBuilderLegacy.build(BenchmarkTypes.AnnotatedPacket.class);
        blackhole.consume(codec);
    }

    @Benchmark
    public void build_annotatedPacket_generated(Blackhole blackhole) {
        StreamCodec<?, ?> codec = NetworkRecordCodecBuilder.build(BenchmarkTypes.AnnotatedPacket.class);
        blackhole.consume(codec);
    }
}
