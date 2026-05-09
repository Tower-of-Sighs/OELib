package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigIntLens;
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
public class ConfigLensBenchmark {

    @Benchmark
    public void updateByGeneratedLens(LensState state, Blackhole blackhole) {
        Root updated = state.rootCountLens.update(state.sample, value -> value + 1);
        blackhole.consume(updated);
    }

    @Benchmark
    public void updateByGeneratedLensInt(LensState state, Blackhole blackhole) {
        Root updated = state.rootCountIntLens.update(state.sample, value -> value + 1);
        blackhole.consume(updated);
    }

    @Benchmark
    public void updateManually(LensState state, Blackhole blackhole) {
        Root s = state.sample;
        Root updated = new Root(new Inner(s.inner().count() + 1, s.inner().enabled()), s.opt());
        blackhole.consume(updated);
    }

    @Benchmark
    public void ifPresentByPrism(LensState state, Blackhole blackhole) {
        Root updated = state.optionalPrism.updateIfPresent(state.sample, value -> value + 1);
        blackhole.consume(updated);
    }

    @Benchmark
    public void cachedLensLookup(Blackhole blackhole) {
        blackhole.consume(RecordLensBuilder.lens(Root.class, Root::inner));
    }

    @State(Scope.Thread)
    public static class LensState {
        ConfigLens<Root, Inner> innerLens = RecordLensBuilder.lens(Root.class, Root::inner);
        ConfigLens<Inner, Integer> countLens = RecordLensBuilder.lens(Inner.class, Inner::count);
        ConfigLens<Root, Integer> rootCountLens = innerLens.compose(countLens);
        ConfigIntLens<Root> rootCountIntLens = rootCountLens.asInt();
        ConfigLens<Root, Optional<Integer>> optionalLens = RecordLensBuilder.lens(Root.class, Root::opt);
        ConfigPrism<Root, Integer> optionalPrism = RecordLensBuilder.optional(optionalLens);
        Root sample = new Root(new Inner(4, true), Optional.of(8));
    }

    private record Root(Inner inner, Optional<Integer> opt) {
    }

    private record Inner(int count, boolean enabled) {
    }
}
