package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.testsupport.TestFileUtil;
import cc.sighs.oelib.config.testsupport.TestPlatform;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConfigAccessTest {
    @BeforeEach
    void clean() throws Exception {
        TestFileUtil.cleanDirectory(TestPlatform.CONFIG_PATH);
        Files.createDirectories(TestPlatform.CONFIG_PATH);
    }

    @Test
    void accessorAndLensSetBothWork() {
        String fileName = "access_" + UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                new ResourceLocation("oelibtest", "access"),
                AccessConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.bool("enabled").defaultValue(true).forGetter(AccessConfig::enabled),
                        ConfigField.intRange("level", 0, 10).defaultValue(1).forGetter(AccessConfig::level)
                ).apply(schema, AccessConfig::new)
        );

        ConfigAccess<AccessConfig> access = new ConfigAccess<>(def.unit());
        access.set(AccessConfig::enabled, false);
        assertFalse(def.unit().get().enabled());

        var levelLens = def.lens(AccessConfig::level);
        access.set(levelLens, 7);
        assertEquals(7, def.unit().get().level());
    }

    private record AccessConfig(boolean enabled, int level) {
    }
}
