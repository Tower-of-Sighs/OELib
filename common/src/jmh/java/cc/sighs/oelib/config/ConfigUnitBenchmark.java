package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class ConfigUnitBenchmark {

    @Benchmark
    public void updateWithSave(UnitState state, Blackhole blackhole) {
        BenchConfig next = state.unit.update(BenchConfig::count, value -> value + 1);
        blackhole.consume(next);
    }

    @Benchmark
    public void updateNoSave(UnitState state, Blackhole blackhole) {
        BenchConfig next = ConfigUnitOps.updateNoSave(state.unit, BenchConfig::count, value -> value + 1);
        blackhole.consume(next);
    }

    @Benchmark
    public void updateIntNoSave(UnitState state, Blackhole blackhole) {
        BenchConfig next = ConfigUnitOps.updateIntNoSave(state.unit, BenchConfig::count, value -> value + 1);
        blackhole.consume(next);
    }

    @Benchmark
    public void ifPresentWithSave(UnitState state, Blackhole blackhole) {
        BenchConfig next = state.unit.ifPresent(BenchConfig::opt, value -> value + 1);
        blackhole.consume(next);
    }

    @Benchmark
    public void batchWithSingleSave(UnitState state, Blackhole blackhole) {
        BenchConfig next = ConfigUnitOps.withBatch(state.unit, batch -> {
            batch.updateInt(BenchConfig::count, value -> value + 1);
            batch.updateInt(BenchConfig::count, value -> value + 1);
            batch.updateInt(BenchConfig::count, value -> value + 1);
        });
        blackhole.consume(next);
    }

    @Benchmark
    public void sequentialWithSave(UnitState state, Blackhole blackhole) {
        BenchConfig next = state.unit.update(BenchConfig::count, value -> value + 1);
        next = state.unit.update(BenchConfig::count, value -> value + 1);
        next = state.unit.update(BenchConfig::count, value -> value + 1);
        next = state.unit.ifPresent(BenchConfig::opt, value -> value + 1);
        blackhole.consume(next);
    }

    @State(Scope.Thread)
    public static class UnitState {
        ConfigUnit<BenchConfig> unit;

        @Setup(Level.Trial)
        public void setup() {
            String suffix = UUID.randomUUID().toString().replace("-", "");
            var definition = ConfigSchema.defineClient(
                    Identifier.fromNamespaceAndPath("oelibjmh", "unit_" + suffix),
                    BenchConfig.class,
                    meta -> meta.fileName("jmh_unit_" + suffix).directory("oelib-jmh").format(ConfigStorageFormat.JSON),
                    schema -> schema.group(
                            ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(BenchConfig::count),
                            ConfigField.optional("opt", Codec.INT).forGetter(BenchConfig::opt)
                    ).apply(schema, BenchConfig::new)
            );
            unit = definition.unit();
            unit.get();
        }
    }

    private record BenchConfig(int count, Optional<Integer> opt) {
    }
}
