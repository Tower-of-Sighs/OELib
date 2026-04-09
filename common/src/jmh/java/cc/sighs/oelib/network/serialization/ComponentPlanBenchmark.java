package cc.sighs.oelib.network.serialization;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for {@link ComponentIO#planOf(Class, java.lang.reflect.Type)}.
 * <p>
 * This is mostly "startup cost" for {@link NetworkRecordCodecBuilder}, so keep it measured separately.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class ComponentPlanBenchmark {

    @Benchmark
    public void plan_int(Blackhole blackhole) {
        blackhole.consume(ComponentIO.planOf(int.class, int.class));
    }

    @Benchmark
    public void plan_uuidList(Blackhole blackhole) {
        blackhole.consume(ComponentIO.planOf(java.util.List.class, BenchmarkTypes.LIST_UUID));
    }

    @Benchmark
    public void plan_optionalString(Blackhole blackhole) {
        blackhole.consume(ComponentIO.planOf(java.util.Optional.class, BenchmarkTypes.OPTIONAL_STRING));
    }

    @Benchmark
    public void plan_optionalListInt(Blackhole blackhole) {
        blackhole.consume(ComponentIO.planOf(java.util.Optional.class, BenchmarkTypes.OPTIONAL_LIST_INT));
    }
}

