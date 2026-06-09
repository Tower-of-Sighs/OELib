package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.optics.ConfigAffine;
import cc.sighs.oelib.config.optics.ConfigLens;
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

        ConfigUnit<UnitStressConfig> unit = definition.unit();
        ConfigLens<UnitStressConfig, Integer> countLens = definition.lens(UnitStressConfig::count);
        ConfigLens<UnitStressConfig, Optional<Integer>> optLens = definition.lens(UnitStressConfig::opt);
        ConfigAffine<UnitStressConfig, Integer> optPrism = RecordLensBuilder.optional(optLens);

        unit.updateAll(countLens.setTo(1), optLens.setTo(Optional.of(2)));
        for (int i = 0; i < 2_000; i++) {
            unit.updateAll(
                    countLens.map(v -> v + 1),
                    optLens.map(v -> v.map(x -> x + 1))
            );
            unit.ifPresent(optPrism, v -> v + 1);
        }

        assertEquals(2_001, unit.view(countLens));
        assertEquals(Optional.of(4_002), unit.view(optLens));
    }

    private record UnitStressConfig(int count, Optional<Integer> opt) {
    }
}
