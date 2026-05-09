package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigLens;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class ConfigLensStressBenchmark {

    @Benchmark
    public void deepHotPathUpdate(StateData state, Blackhole blackhole) {
        Root out = state.base;
        for (int i = 0; i < 64; i++) {
            out = state.valueLens.update(out, v -> v + 1);
        }
        blackhole.consume(out);
    }

    @Benchmark
    public void deepHotPathUpdateManual(StateData state, Blackhole blackhole) {
        Root out = state.base;
        for (int i = 0; i < 64; i++) {
            L4 l4 = out.l1().l2().l3().l4();
            L3 l3 = out.l1().l2().l3();
            L2 l2 = out.l1().l2();
            L1 l1 = out.l1();
            out = new Root(new L1(new L2(new L3(new L4(l4.value() + 1, l4.flag()), l3.pad()), l2.ratio(), l2.note()), l1.seed()), out.meta());
        }
        blackhole.consume(out);
    }

    @Benchmark
    public void multiPointMutation(StateData state, Blackhole blackhole) {
        Root out = state.base;
        for (int i = 0; i < 16; i++) {
            out = state.valueLens.update(out, v -> v + 2);
            out = state.flagLens.update(out, v -> !v);
            out = state.ratioLens.update(out, v -> v + 0.01);
            out = state.seedLens.update(out, v -> v + 1);
        }
        blackhole.consume(out);
    }

    @State(Scope.Thread)
    public static class StateData {
        Root base = new Root(new L1(new L2(new L3(new L4(4, false), 9), 1.25, "n"), 6), "m");

        ConfigLens<Root, L1> l1 = RecordLensBuilder.lens(Root.class, Root::l1);
        ConfigLens<L1, L2> l2 = RecordLensBuilder.lens(L1.class, L1::l2);
        ConfigLens<L2, L3> l3 = RecordLensBuilder.lens(L2.class, L2::l3);
        ConfigLens<L3, L4> l4 = RecordLensBuilder.lens(L3.class, L3::l4);

        ConfigLens<Root, Integer> valueLens = l1.compose(l2).compose(l3).compose(l4).compose(RecordLensBuilder.lens(L4.class, L4::value));
        ConfigLens<Root, Boolean> flagLens = l1.compose(l2).compose(l3).compose(l4).compose(RecordLensBuilder.lens(L4.class, L4::flag));
        ConfigLens<Root, Double> ratioLens = l1.compose(l2).compose(RecordLensBuilder.lens(L2.class, L2::ratio));
        ConfigLens<Root, Integer> seedLens = l1.compose(RecordLensBuilder.lens(L1.class, L1::seed));
    }

    private record Root(L1 l1, String meta) {
    }

    private record L1(L2 l2, int seed) {
    }

    private record L2(L3 l3, double ratio, String note) {
    }

    private record L3(L4 l4, int pad) {
    }

    private record L4(int value, boolean flag) {
    }
}
