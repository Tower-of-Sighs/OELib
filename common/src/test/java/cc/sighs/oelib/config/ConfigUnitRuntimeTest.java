package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.optics.ConfigIntLens;
import cc.sighs.oelib.config.optics.ConfigPrism;
import cc.sighs.oelib.config.testsupport.TestFileUtil;
import cc.sighs.oelib.config.testsupport.TestPlatform;
import cc.sighs.oelib.config.util.ConfigIOUtil;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigUnitRuntimeTest {
    private static Dynamic<?> absNumberMigration(Dynamic<?> dynamic) {
        int raw = dynamic.asInt(0);
        return dynamic.createInt(Math.abs(raw));
    }

    @BeforeEach
    void clean() throws Exception {
        TestFileUtil.cleanDirectory(TestPlatform.CONFIG_PATH);
        Files.createDirectories(TestPlatform.CONFIG_PATH);
    }

    @Test
    void updateSaveValidateAndMigrateWorkTogether() throws Exception {
        String fileName = "config_" + UUID.randomUUID().toString().replace("-", "");
        var definition = ConfigSchema.defineClient(
                Identifier.fromNamespaceAndPath("oelibtest", "runtime"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE)
                                .defaultValue(1)
                                .validate((value, _) -> value != null && value <= 10 ? Optional.empty() : Optional.of("count must be <= 10"))
                                .migrate(1, ConfigUnitRuntimeTest::absNumberMigration)
                                .forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );

        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var countLens = definition.lens(RuntimeConfig::count);
        var optLens = definition.lens(RuntimeConfig::opt);
        var optPrism = RecordLensBuilder.optional(optLens);

        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());
        Files.createDirectories(savePath.getParent());
        Files.writeString(savePath, "{\"count\":-4,\"opt\":5}", StandardCharsets.UTF_8);

        RuntimeConfig loaded = unit.get();
        assertEquals(4, loaded.count());
        assertEquals(Optional.of(5), loaded.opt());

        unit.update(countLens, value -> value + 1);
        unit.ifPresent(optPrism, value -> value + 2);
        assertEquals(5, unit.view(countLens));
        assertEquals(Optional.of(7), unit.view(optLens));

        assertThrows(IllegalStateException.class, () -> unit.update(countLens, ignored -> 99));
        assertEquals(5, unit.view(countLens));

        unit.setValue(new RuntimeConfig(123, Optional.of(9)));
        unit.save();
        assertEquals(5, unit.view(countLens));
        assertEquals(Optional.of(7), unit.view(optLens));
        assertTrue(Files.exists(savePath));
    }

    @Test
    void subtypePrismUpdatesOnlyMatchingSubtype() {
        var modeLens = RecordLensBuilder.lens(ModeHolder.class, ModeHolder::mode);
        var aPrism = RecordLensBuilder.subtype(modeLens, ModeA.class);

        ModeHolder aSource = new ModeHolder(new ModeA(2));
        ModeHolder bSource = new ModeHolder(new ModeB("x"));

        ModeHolder aUpdated = aPrism.updateIfPresent(aSource, mode -> new ModeA(mode.value() + 1));
        ModeHolder bUpdated = aPrism.updateIfPresent(bSource, mode -> new ModeA(mode.value() + 1));

        assertEquals(3, ((ModeA) aUpdated.mode()).value());
        assertSame(bSource.mode(), bUpdated.mode());
    }

    @Test
    void noSaveUpdatesOnlyPersistAfterExplicitSave() throws Exception {
        String fileName = "nosave_" + UUID.randomUUID().toString().replace("-", "");
        var definition = ConfigSchema.defineClient(
                Identifier.fromNamespaceAndPath("oelibtest", "nosave"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );
        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var countLens = definition.lens(RuntimeConfig::count);
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        ConfigUnitOps.updateNoSave(unit, countLens, value -> value + 4);
        assertEquals(5, unit.view(countLens));
        assertFalse(Files.exists(savePath));

        unit.save();
        assertTrue(Files.exists(savePath));
        String content = Files.readString(savePath, StandardCharsets.UTF_8);
        assertEquals(5, JsonParser.parseString(content).getAsJsonObject().get("count").getAsInt());
    }

    @Test
    void batchUpdatePersistsOnceAfterMultipleMutations() {
        String fileName = "batch_" + UUID.randomUUID().toString().replace("-", "");
        var definition = ConfigSchema.defineClient(
                Identifier.fromNamespaceAndPath("oelibtest", "batch"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );

        ConfigUnit<RuntimeConfig> unit = definition.unit();
        ConfigIntLens<RuntimeConfig> countLens = definition.lens(RuntimeConfig::count).asInt();
        ConfigPrism<RuntimeConfig, Integer> optPrism = RecordLensBuilder.optional(definition.lens(RuntimeConfig::opt));
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        ConfigUnitOps.withBatchNoSave(unit, batch -> {
            batch.updateInt(countLens, value -> value + 3);
            batch.ifPresent(optPrism, value -> value + 1);
        });
        assertFalse(Files.exists(savePath));

        ConfigUnitOps.withBatch(unit, batch -> {
            batch.updateInt(countLens, value -> value + 2);
            batch.ifPresent(optPrism, value -> value + 1);
        });

        assertTrue(Files.exists(savePath));
        assertEquals(6, unit.view(definition.lens(RuntimeConfig::count)));
        assertEquals(Optional.empty(), unit.view(definition.lens(RuntimeConfig::opt)));
    }

    @Test
    void setAndGetPersistsAndReturnsUpdatedValue() throws Exception {
        String fileName = "setandget_" + UUID.randomUUID().toString().replace("-", "");
        var definition = ConfigSchema.defineClient(
                Identifier.fromNamespaceAndPath("oelibtest", "setandget"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );
        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var countLens = definition.lens(RuntimeConfig::count);
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        int updated = ConfigUnitOps.setAndGet(unit, countLens, 11);
        assertEquals(11, updated);
        assertEquals(11, unit.view(countLens));
        assertTrue(Files.exists(savePath));
        String content = Files.readString(savePath, StandardCharsets.UTF_8);
        assertEquals(11, JsonParser.parseString(content).getAsJsonObject().get("count").getAsInt());
    }

    @Test
    void setAndGetNoSaveDefersPersistenceUntilExplicitSave() throws Exception {
        String fileName = "setandget_nosave_" + UUID.randomUUID().toString().replace("-", "");
        var definition = ConfigSchema.defineClient(
                Identifier.fromNamespaceAndPath("oelibtest", "setandget_nosave"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );
        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var countLens = definition.lens(RuntimeConfig::count);
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        int updated = ConfigUnitOps.setAndGetNoSave(unit, countLens, 13);
        assertEquals(13, updated);
        assertEquals(13, unit.view(countLens));
        assertFalse(Files.exists(savePath));

        unit.save();
        assertTrue(Files.exists(savePath));
        String content = Files.readString(savePath, StandardCharsets.UTF_8);
        assertEquals(13, JsonParser.parseString(content).getAsJsonObject().get("count").getAsInt());
    }

    @Test
    void batchSetAndGetWorksForNoSaveAndSaveModes() throws Exception {
        String fileName = "batch_setandget_" + UUID.randomUUID().toString().replace("-", "");
        var definition = ConfigSchema.defineClient(
                Identifier.fromNamespaceAndPath("oelibtest", "batch_setandget"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );
        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var countLens = definition.lens(RuntimeConfig::count);
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        ConfigUnitOps.withBatchNoSave(unit, batch -> {
            int noSaveValue = batch.setAndGet(countLens, 17);
            assertEquals(17, noSaveValue);
        });
        assertEquals(17, unit.view(countLens));
        assertFalse(Files.exists(savePath));

        ConfigUnitOps.withBatch(unit, batch -> {
            int savedValue = batch.setAndGet(countLens, 19);
            assertEquals(19, savedValue);
        });
        assertEquals(19, unit.view(countLens));
        assertTrue(Files.exists(savePath));
        String content = Files.readString(savePath, StandardCharsets.UTF_8);
        assertEquals(19, JsonParser.parseString(content).getAsJsonObject().get("count").getAsInt());
    }

    private sealed interface Mode permits ModeA, ModeB {
    }

    private record RuntimeConfig(int count, Optional<Integer> opt) {
    }

    private record ModeA(int value) implements Mode {
    }

    private record ModeB(String value) implements Mode {
    }

    private record ModeHolder(Mode mode) {
    }
}
