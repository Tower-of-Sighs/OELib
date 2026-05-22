package cc.sighs.oelib.event.benchmark;

import cc.sighs.oelib.event.Event;
import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.Subscribe;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, time = 150, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class EventBusConcurrentBenchmarks {

    @Param({"32", "256"})
    public int listenerCount;

    private static void baselineSink(BaseEvent e, Blackhole bh) {
        bh.consume(e);
    }

    @Setup(Level.Trial)
    public void setup() {
        for (int i = 0; i < listenerCount; i++) {
            EventBus.register(new InstanceListener());
        }
    }

    @Benchmark
    @Threads(4)
    public void postConcurrent4(Blackhole bh) {
        bh.consume(EventBus.post(new BaseEvent()));
    }

    @Benchmark
    @Threads(16)
    public void postConcurrent16(Blackhole bh) {
        bh.consume(EventBus.post(new BaseEvent()));
    }

    @Benchmark
    @Threads(4)
    public void postWithInvalidation4(Blackhole bh) {
        EventBus.register(StaticListener.class);
        EventBus.unregister(StaticListener.class);
        bh.consume(EventBus.post(new BaseEvent()));
    }

    @Benchmark
    public void baselineDirectInvoke(Blackhole bh) {
        baselineSink(new BaseEvent(), bh);
    }

    public static class BaseEvent implements Event {}

    public static class InstanceListener {
        @Subscribe
        public void on(BaseEvent e) {}
    }

    public static class StaticListener {
        @Subscribe
        public static void on(BaseEvent e) {}
    }
}

