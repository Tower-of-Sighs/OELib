package cc.sighs.oelib.event.benchmark;

import cc.sighs.oelib.event.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class EventBusJmhBenchmarks {

    @Param({"1", "32", "256"})
    public int listenerCount;

    @Param({"0", "2"})
    public int depth;

    @Param({"false", "true"})
    public boolean cancellable;

    @Param({"false", "true"})
    public boolean mixedPhases;

    @Setup(Level.Trial)
    public void setup() {

        if (mixedPhases) {
            for (int i = 0; i < listenerCount; i++) {
                EventBus.register(new MixedPhasesInstance());
            }
        } else {
            for (int i = 0; i < listenerCount; i++) {
                EventBus.register(new InstanceListener());
            }
        }
    }

    @Benchmark
    @Threads(1)
    public void post(Blackhole bh) {
        Event event = createEvent();
        bh.consume(EventBus.post(event));
    }

    private Event createEvent() {

        if (!cancellable) {

            if (depth == 0) return new BaseEvent();
            if (depth == 1) return new ChildEvent();

            return new GrandChildEvent();
        }

        return new MyCancellableEvent();
    }

    public static class BaseEvent implements Event {}

    public static class ChildEvent extends BaseEvent {}

    public static class GrandChildEvent extends ChildEvent {}

    public static class MyCancellableEvent implements CancellableEvent {

        private boolean canceled;

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    public static class InstanceListener {

        @Subscribe
        public void onBase(BaseEvent e) {}

        @Subscribe
        public void onChild(ChildEvent e) {}

        @Subscribe
        public void onGrand(GrandChildEvent e) {}

        @Subscribe
        public void onCancellable(MyCancellableEvent e) {}
    }

    public static class MixedPhasesInstance {

        @Subscribe(phase = EventPhase.PRE, priority = EventPriority.HIGH)
        public void a(BaseEvent e) {}

        @Subscribe(phase = EventPhase.NORMAL)
        public void b(BaseEvent e) {}

        @Subscribe(phase = EventPhase.POST, priority = EventPriority.LOW)
        public void c(BaseEvent e) {}

        @Subscribe(phase = EventPhase.NORMAL)
        public void onCancellable(MyCancellableEvent e) {}
    }
}
