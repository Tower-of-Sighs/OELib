package cc.sighs.oelib.network.serialization;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for custom codec resolution driven by annotations:
 * {@link NetFieldCodec}, {@link JsonCodec}, {@link RegistryCodec}.
 * <p>
 * Note: cold benchmarks clear internal caches, so they also include the cost of clearing small maps.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class CustomCodecResolverBenchmark {

    @Benchmark
    public void resolve_cached_netDirect(ResolverState state, Blackhole blackhole) {
        blackhole.consume(CustomCodecResolver.resolve(BenchmarkTypes.AnnotatedPacket.class, state.netDirect));
    }

    @Benchmark
    public void resolve_cached_netScanned(ResolverState state, Blackhole blackhole) {
        blackhole.consume(CustomCodecResolver.resolve(BenchmarkTypes.AnnotatedPacket.class, state.netScanned));
    }

    @Benchmark
    public void resolve_cached_jsonDirect(ResolverState state, Blackhole blackhole) {
        blackhole.consume(CustomCodecResolver.resolve(BenchmarkTypes.AnnotatedPacket.class, state.jsonDirect));
    }

    @Benchmark
    public void resolve_cached_jsonScanned(ResolverState state, Blackhole blackhole) {
        blackhole.consume(CustomCodecResolver.resolve(BenchmarkTypes.AnnotatedPacket.class, state.jsonScanned));
    }

    @Benchmark
    public void resolve_cached_registry(ResolverState state, Blackhole blackhole) {
        blackhole.consume(CustomCodecResolver.resolve(BenchmarkTypes.AnnotatedPacket.class, state.registry));
    }

    @Benchmark
    public void resolve_cold_netScanned(ResolverState state, Blackhole blackhole) {
        state.clearCaches();
        blackhole.consume(CustomCodecResolver.resolve(BenchmarkTypes.AnnotatedPacket.class, state.netScanned));
    }

    @State(Scope.Thread)
    public static class ResolverState {
        RecordComponent netDirect;
        RecordComponent netScanned;
        RecordComponent jsonDirect;
        RecordComponent jsonScanned;
        RecordComponent registry;

        ConcurrentHashMap<?, ?> netCache;
        ConcurrentHashMap<?, ?> registryCache;
        ConcurrentHashMap<?, ?> jsonCache;

        private static Object staticField(Class<?> type, String fieldName) {
            try {
                Field f = type.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Setup(Level.Trial)
        public void setup() {
            var components = BenchmarkTypes.AnnotatedPacket.class.getRecordComponents();
            netDirect = components[0];
            netScanned = components[1];
            jsonDirect = components[2];
            jsonScanned = components[3];
            registry = components[4];

            netCache = (ConcurrentHashMap<?, ?>) staticField(CustomCodecResolver.class, "CACHE");
            registryCache = (ConcurrentHashMap<?, ?>) staticField(CustomCodecResolver.class, "REGISTRY_CACHE");
            jsonCache = (ConcurrentHashMap<?, ?>) staticField(CustomCodecResolver.class, "JSON_CACHE");
        }

        void clearCaches() {
            netCache.clear();
            registryCache.clear();
            jsonCache.clear();
        }
    }
}

