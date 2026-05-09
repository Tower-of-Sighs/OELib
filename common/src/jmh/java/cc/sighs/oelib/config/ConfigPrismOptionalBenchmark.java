package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigLens;
import cc.sighs.oelib.config.optics.ConfigPrism;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class ConfigPrismOptionalBenchmark {

    @Benchmark
    public void updateIfPresent_present(PrismState state, Blackhole blackhole) {
        OptionalRoot out = state.prism.updateIfPresent(state.present, value -> value + 1);
        blackhole.consume(out);
    }

    @Benchmark
    public void updateIfPresent_absent(PrismState state, Blackhole blackhole) {
        OptionalRoot out = state.prism.updateIfPresent(state.absent, value -> value + 1);
        blackhole.consume(out);
    }

    @Benchmark
    public void manual_present(PrismState state, Blackhole blackhole) {
        Optional<Integer> value = state.present.value();
        OptionalRoot out = value.map(integer -> new OptionalRoot(Optional.of(integer + 1), state.present.flag())).orElseGet(() -> state.present);
        blackhole.consume(out);
    }

    @Benchmark
    public void manual_absent(PrismState state, Blackhole blackhole) {
        Optional<Integer> value = state.absent.value();
        OptionalRoot out = value.map(integer -> new OptionalRoot(Optional.of(integer + 1), state.absent.flag())).orElseGet(() -> state.absent);
        blackhole.consume(out);
    }

    @State(Scope.Thread)
    public static class PrismState {
        OptionalRoot present = new OptionalRoot(Optional.of(10), true);
        OptionalRoot absent = new OptionalRoot(Optional.empty(), true);
        ConfigLens<OptionalRoot, Optional<Integer>> lens = RecordLensBuilder.lens(OptionalRoot.class, OptionalRoot::value);
        ConfigPrism<OptionalRoot, Integer> prism = RecordLensBuilder.optional(lens);
    }

    private record OptionalRoot(Optional<Integer> value, boolean flag) {
    }
}
