package cc.sighs.oelib.network.serialization;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Microbenchmarks for the hot-path overhead that mattered most in the old framework:
 * Method.invoke / Constructor.newInstance vs MethodHandle invocation.
 * <p>
 * This is intentionally independent of Minecraft buffer types so it can run in isolation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
public class RecordInvokeBenchmark {

    @Benchmark
    public void getters_reflection(InvokeState state, Blackhole blackhole) throws Exception {
        for (Method m : state.reflectGetters) {
            blackhole.consume(m.invoke(state.value));
        }
    }

    @Benchmark
    public void getters_methodHandles(InvokeState state, Blackhole blackhole) throws Throwable {
        for (MethodHandle mh : state.mhGetters) {
            blackhole.consume(mh.invoke(state.value));
        }
    }

    @Benchmark
    public void ctor_reflection(InvokeState state, Blackhole blackhole) throws Exception {
        blackhole.consume(state.reflectCtor.newInstance(state.ctorArgs));
    }

    @Benchmark
    public void ctor_methodHandles(InvokeState state, Blackhole blackhole) throws Throwable {
        blackhole.consume(state.mhCtor.invoke(state.ctorArgs));
    }

    @State(Scope.Thread)
    public static class InvokeState {
        BenchmarkTypes.PlainPacket value;

        Method[] reflectGetters;
        MethodHandle[] mhGetters;

        Constructor<BenchmarkTypes.PlainPacket> reflectCtor;
        MethodHandle mhCtor;

        Object[] ctorArgs;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            value = new BenchmarkTypes.PlainPacket(
                    123,
                    456L,
                    "hello",
                    new UUID(1, 2),
                    Optional.of(List.of(1, 2, 3, 4))
            );

            var recordClass = BenchmarkTypes.PlainPacket.class;
            var components = recordClass.getRecordComponents();

            reflectGetters = new Method[components.length];
            mhGetters = new MethodHandle[components.length];

            var lookup = MethodHandles.privateLookupIn(recordClass, MethodHandles.lookup());
            Class<?>[] ctorParamTypes = new Class<?>[components.length];
            ctorArgs = new Object[components.length];

            for (int i = 0; i < components.length; i++) {
                var c = components[i];
                ctorParamTypes[i] = c.getType();
                reflectGetters[i] = c.getAccessor();
                reflectGetters[i].setAccessible(true);
                mhGetters[i] = lookup
                        .findVirtual(recordClass, c.getName(), MethodType.methodType(c.getType()))
                        .asType(MethodType.methodType(Object.class, Object.class));
            }

            reflectCtor = recordClass.getDeclaredConstructor(ctorParamTypes);
            reflectCtor.setAccessible(true);

            mhCtor = lookup
                    .findConstructor(recordClass, MethodType.methodType(void.class, ctorParamTypes))
                    .asSpreader(Object[].class, components.length)
                    .asType(MethodType.methodType(Object.class, Object[].class));

            ctorArgs[0] = 123;
            ctorArgs[1] = 456L;
            ctorArgs[2] = "hello";
            ctorArgs[3] = new UUID(1, 2);
            ctorArgs[4] = Optional.of(List.of(1, 2, 3, 4));
        }
    }
}

