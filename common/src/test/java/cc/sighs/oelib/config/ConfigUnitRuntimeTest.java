package cc.sighs.oelib.config;

import cc.sighs.oelib.config.codecs.ConfigMetaCodec;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.testsupport.TestFileUtil;
import cc.sighs.oelib.config.testsupport.TestPlatform;
import cc.sighs.oelib.config.util.ConfigIOUtil;
import cc.sighs.oelib.config.util.ConfigSerializationUtil;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
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
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "runtime"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE)
                                .defaultValue(1)
                                .validate((value, object) -> value != null && value <= 10 ? Optional.empty() : Optional.of("count must be <= 10"))
                                .migrate(1, ConfigUnitRuntimeTest::absNumberMigration)
                                .forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );

        ConfigUnit<RuntimeConfig> unit = definition.unit();

        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());
        Files.createDirectories(savePath.getParent());
        Files.writeString(savePath, "{\"count\":-4,\"opt\":5}", StandardCharsets.UTF_8);

        RuntimeConfig loaded = unit.get();
        assertEquals(4, loaded.count());
        assertEquals(Optional.of(5), loaded.opt());

        unit.update(RuntimeConfig::count, value -> value + 1);
        unit.ifPresent(RuntimeConfig::opt, value -> value + 2);
        assertEquals(5, unit.get().count());
        assertEquals(Optional.of(7), unit.get().opt());

        assertThrows(IllegalStateException.class, () -> unit.update(RuntimeConfig::count, ignored -> 99));
        assertEquals(5, unit.get().count());

        unit.setValue(new RuntimeConfig(123, Optional.of(9)));
        unit.save();
        assertEquals(5, unit.get().count());
        assertEquals(Optional.of(7), unit.get().opt());
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
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "nosave"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );
        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        ConfigUnitOps.updateNoSave(unit, RuntimeConfig::count, value -> value + 4);
        assertEquals(5, unit.get().count());
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
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "batch"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );

        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var optPrism = RecordLensBuilder.optional(definition.lens(RuntimeConfig::opt));
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        ConfigUnitOps.withBatchNoSave(unit, batch -> {
            batch.updateInt(RuntimeConfig::count, value -> value + 3);
            batch.ifPresent(optPrism, value -> value + 1);
        });
        assertFalse(Files.exists(savePath));

        ConfigUnitOps.withBatch(unit, batch -> {
            batch.updateInt(RuntimeConfig::count, value -> value + 2);
            batch.ifPresent(optPrism, value -> value + 1);
        });

        assertTrue(Files.exists(savePath));
        assertEquals(6, unit.get().count());
        assertEquals(Optional.empty(), unit.get().opt());
    }

    @Test
    void setAndGetPersistsAndReturnsUpdatedValue() throws Exception {
        String fileName = "setandget_" + UUID.randomUUID().toString().replace("-", "");
        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "setandget"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );
        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        int updated = ConfigUnitOps.setAndGet(unit, RuntimeConfig::count, 11);
        assertEquals(11, updated);
        assertEquals(11, unit.get().count());
        assertTrue(Files.exists(savePath));
        String content = Files.readString(savePath, StandardCharsets.UTF_8);
        assertEquals(11, JsonParser.parseString(content).getAsJsonObject().get("count").getAsInt());
    }

    @Test
    void setAndGetNoSaveDefersPersistenceUntilExplicitSave() throws Exception {
        String fileName = "setandget_nosave_" + UUID.randomUUID().toString().replace("-", "");
        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "setandget_nosave"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );
        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        int updated = ConfigUnitOps.setAndGetNoSave(unit, RuntimeConfig::count, 13);
        assertEquals(13, updated);
        assertEquals(13, unit.get().count());
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
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "batch_setandget"),
                RuntimeConfig.class,
                meta -> meta.fileName(fileName).directory("unit-tests").format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );
        ConfigUnit<RuntimeConfig> unit = definition.unit();
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        unit.get();
        ConfigUnitOps.withBatchNoSave(unit, batch -> {
            int noSaveValue = batch.setAndGet(RuntimeConfig::count, 17);
            assertEquals(17, noSaveValue);
        });
        assertEquals(17, unit.get().count());
        assertFalse(Files.exists(savePath));

        ConfigUnitOps.withBatch(unit, batch -> {
            int savedValue = batch.setAndGet(RuntimeConfig::count, 19);
            assertEquals(19, savedValue);
        });
        assertEquals(19, unit.get().count());
        assertTrue(Files.exists(savePath));
        String content = Files.readString(savePath, StandardCharsets.UTF_8);
        assertEquals(19, JsonParser.parseString(content).getAsJsonObject().get("count").getAsInt());
    }

    @Test
    void tomlEncodingSupportsListsAndMapsOfNestedRecords() {
        var definition = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", "toml_nested"),
                TomlRoot.class,
                meta -> meta.fileName("toml_nested").directory("unit-tests").format(ConfigStorageFormat.TOML),
                schema -> schema.group(
                        ConfigField.list("entries", TomlEntry.META_CODEC)
                                .defaultValue(List.of(
                                        new TomlEntry("alpha", 1, Optional.of(new TomlNested(true, 2))),
                                        new TomlEntry("beta", 2, Optional.empty())
                                ))
                                .forGetter(TomlRoot::entries),
                        ConfigField.map("entryMap", Codec.STRING, TomlEntry.META_CODEC)
                                .defaultValue(Map.of(
                                        "left", new TomlEntry("left", 3, Optional.empty()),
                                        "right", new TomlEntry("right", 4, Optional.of(new TomlNested(false, 5)))
                                ))
                                .forGetter(TomlRoot::entryMap)
                ).apply(schema, TomlRoot::new)
        );

        TomlRoot value = definition.unit().getDefaultValue();
        String encoded = ConfigSerializationUtil.encodeToString(
                value,
                ConfigStorageFormat.TOML,
                definition.unit().codec().codec(),
                definition.unit().codec().fields()
        ).orElseThrow();

        TomlRoot decoded = ConfigSerializationUtil.parse(
                encoded,
                ConfigStorageFormat.TOML,
                definition.unit().codec().codec()
        ).result().orElseThrow();

        assertEquals(value, decoded);
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

    private record TomlRoot(List<TomlEntry> entries, Map<String, TomlEntry> entryMap) {
    }

    private record TomlNested(boolean enabled, int level) {
    }

    private record TomlEntry(String id, int weight, Optional<TomlNested> extra) {
        private static final ConfigMetaCodec<TomlEntry> META_CODEC = ConfigSchema.metaCodec(
                TomlEntry.class,
                schema -> schema.group(
                        ConfigField.string("id").defaultValue("entry").forGetter(TomlEntry::id),
                        ConfigField.intRange("weight", 0, 100).defaultValue(0).forGetter(TomlEntry::weight),
                        ConfigField.optional("extra", TOML_NESTED_CODEC).forGetter(TomlEntry::extra)
                ).apply(schema, TomlEntry::new)
        );
    }

    private static final ConfigMetaCodec<TomlNested> TOML_NESTED_CODEC = ConfigSchema.metaCodec(
            TomlNested.class,
            schema -> schema.group(
                    ConfigField.bool("enabled").defaultValue(true).forGetter(TomlNested::enabled),
                    ConfigField.intRange("level", 0, 10).defaultValue(0).forGetter(TomlNested::level)
            ).apply(schema, TomlNested::new)
    );
}
