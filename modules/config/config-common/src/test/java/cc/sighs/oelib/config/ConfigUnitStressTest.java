package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.testsupport.TestFileUtil;
import cc.sighs.oelib.config.testsupport.TestPlatform;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigUnitStressTest {

    @BeforeEach
    void clean() throws Exception {
        TestFileUtil.cleanDirectory(TestPlatform.CONFIG_PATH);
        Files.createDirectories(TestPlatform.CONFIG_PATH);
    }

    @Test
    void updateAllAndIfPresentRemainStableUnderLoad() {
        String fileName = "unit_stress_" + UUID.randomUUID().toString().replace("-", "");
        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", "unit_stress"),
                UnitStressConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(0).forGetter(UnitStressConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(UnitStressConfig::opt)
                ).apply(schema, UnitStressConfig::new)
        );

        ConfigUnit<UnitStressConfig> unit = definition;
        unit.applyMutation(unit.mutation()
                .set(UnitStressConfig::count, 1)
                .set(UnitStressConfig::opt, Optional.of(2)));
        for (int i = 0; i < 2_000; i++) {
            unit.applyMutation(unit.mutation()
                    .map(UnitStressConfig::count, v -> v + 1)
                    .map(UnitStressConfig::opt, v -> v.map(x -> x + 1)));
            unit.ifPresent(UnitStressConfig::opt, v -> v + 1);
        }

        assertEquals(2_001, unit.view(UnitStressConfig::count));
        assertEquals(Optional.of(4_002), unit.view(UnitStressConfig::opt));
    }

    private record UnitStressConfig(int count, Optional<Integer> opt) {
    }
}
