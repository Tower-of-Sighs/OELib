package cc.sighs.oelib.config;

import cc.sighs.oelib.config.codecs.ConfigMetaCodec;
import cc.sighs.oelib.config.codecs.ConfigSealedCodec;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConfigSealedFieldTest {

    @Test
    void sealedFieldDerivesDefaultAndCollectsNestedMeta() {
        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", "sealed_field"),
                SealedRoot.class,
                meta -> meta.fileName("sealed_field").directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.sealed("mode", MODE_CODEC).forGetter(SealedRoot::mode),
                        ConfigField.string("name").defaultValue("demo").forGetter(SealedRoot::name)
                ).apply(schema, SealedRoot::new)
        );

        assertEquals(new SealedRoot(new LevelMode(2), "demo"), definition.unit().getDefaultValue());

        Map<String, ConfigValueMeta> metasByKey = definition.unit().codec().fields().stream()
                .collect(Collectors.toMap(ConfigValueMeta::key, meta -> meta));
        assertEquals(Set.of("mode", "mode.type", "mode.level", "mode.enabled", "name"), metasByKey.keySet());
        assertTrue(metasByKey.get("mode").hidden());
        assertFalse(metasByKey.get("mode.type").hidden());
        assertEquals("mode.type", metasByKey.get("mode.level").visibleWhenPath().orElseThrow());
        assertEquals("level", metasByKey.get("mode.level").visibleWhenValue().orElseThrow());
        assertEquals("mode.type", metasByKey.get("mode.enabled").visibleWhenPath().orElseThrow());
        assertEquals("toggle", metasByKey.get("mode.enabled").visibleWhenValue().orElseThrow());
    }

    @Test
    void pathSubtypeCanUpdateSealedField() {
        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", "sealed_path"),
                SealedRoot.class,
                meta -> meta.fileName("sealed_path").directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.sealed("mode", MODE_CODEC).forGetter(SealedRoot::mode),
                        ConfigField.string("name").defaultValue("demo").forGetter(SealedRoot::name)
                ).apply(schema, SealedRoot::new)
        );

        ConfigUnit<SealedRoot> unit = definition.unit();
        var path = definition.pathSubtype(SealedRoot::mode, LevelMode.class).then(LevelMode::level);

        unit.get();
        SealedRoot updated = ConfigUnitOps.paths(unit).ifPresentNoSave(path, value -> value + 3);
        assertEquals(new LevelMode(5), updated.mode());
    }

    @Test
    void sealedCodecRejectsDuplicateVariantFieldKeys() {
        ConfigMetaCodec<DuplicateModeA> codecA = ConfigSchema.metaCodec(
                DuplicateModeA.class,
                schema -> schema.group(
                        ConfigField.intRange("value", 0, 10).defaultValue(1).forGetter(DuplicateModeA::value)
                ).apply(schema, DuplicateModeA::new)
        );
        ConfigMetaCodec<DuplicateModeB> codecB = ConfigSchema.metaCodec(
                DuplicateModeB.class,
                schema -> schema.group(
                        ConfigField.bool("value").defaultValue(true).forGetter(DuplicateModeB::value)
                ).apply(schema, DuplicateModeB::new)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigSchema.sealedCodec(
                        DuplicateMode.class,
                        "type",
                        mode -> mode instanceof DuplicateModeA ? "a" : "b",
                        new DuplicateModeA(1),
                        ConfigSchema.sealedVariant("a", DuplicateModeA.class, codecA),
                        ConfigSchema.sealedVariant("b", DuplicateModeB.class, codecB)
                )
        );
    }

    private record SealedRoot(Mode mode, String name) {
    }

    private sealed interface Mode permits LevelMode, ToggleMode {
    }

    private record LevelMode(int level) implements Mode {
    }

    private record ToggleMode(boolean enabled) implements Mode {
    }

    private static final ConfigMetaCodec<LevelMode> LEVEL_CODEC = ConfigSchema.metaCodec(
            LevelMode.class,
            schema -> schema.group(
                    ConfigField.intRange("level", 0, 10).defaultValue(2).forGetter(LevelMode::level)
            ).apply(schema, LevelMode::new)
    );

    private static final ConfigMetaCodec<ToggleMode> TOGGLE_CODEC = ConfigSchema.metaCodec(
            ToggleMode.class,
            schema -> schema.group(
                    ConfigField.bool("enabled").defaultValue(true).forGetter(ToggleMode::enabled)
            ).apply(schema, ToggleMode::new)
    );

    private static final ConfigSealedCodec<Mode> MODE_CODEC = ConfigSchema.sealedCodec(
            Mode.class,
            "type",
            mode -> mode instanceof LevelMode ? "level" : "toggle",
            new LevelMode(2),
            ConfigSchema.sealedVariant("level", LevelMode.class, LEVEL_CODEC),
            ConfigSchema.sealedVariant("toggle", ToggleMode.class, TOGGLE_CODEC)
    );

    private sealed interface DuplicateMode permits DuplicateModeA, DuplicateModeB {
    }

    private record DuplicateModeA(int value) implements DuplicateMode {
    }

    private record DuplicateModeB(boolean value) implements DuplicateMode {
    }
}
