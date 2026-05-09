package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigLegacyBuilderTest {

    @Test
    void legacyBuilderDelegatesToSchemaAndBuildsDefaultValue() {
        ConfigUnit<LegacyConfig> unit = ConfigRecordCodecBuilder.createClient(
                Identifier.fromNamespaceAndPath("oelibtest", "legacy_builder"),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE)
                                .defaultValue(7)
                                .forGetter(LegacyConfig::count)
                ).apply(schema, LegacyConfig::new),
                meta -> meta.fileName("legacy_builder").directory("unit-tests").format(ConfigStorageFormat.JSON)
        );

        assertEquals(7, unit.getDefaultValue().count());
        assertEquals(ConfigSide.CLIENT, unit.meta().side());
    }

    private record LegacyConfig(int count) {
    }
}

