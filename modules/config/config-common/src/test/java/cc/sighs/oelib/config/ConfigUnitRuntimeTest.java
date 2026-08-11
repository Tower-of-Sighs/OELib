package cc.sighs.oelib.config;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.testsupport.TestFileUtil;
import cc.sighs.oelib.config.testsupport.TestPlatform;
import cc.sighs.oelib.config.util.ConfigIOUtil;
import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.Subscribe;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConfigUnitRuntimeTest {

    @BeforeEach
    void clean() throws Exception {
        TestFileUtil.cleanDirectory(TestPlatform.CONFIG_PATH);
        Files.createDirectories(TestPlatform.CONFIG_PATH);
    }

    @Test
    void defaultUpdateAutomaticallyPersists() throws Exception {
        ConfigUnit<RuntimeConfig> unit = definition("automatic_save");
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        RuntimeConfig updated = unit.update(RuntimeConfig::count, value -> value + 4);

        assertEquals(5, updated.count());
        assertTrue(Files.exists(savePath));
        String content = Files.readString(savePath, StandardCharsets.UTF_8);
        assertEquals(5, JsonParser.parseString(content).getAsJsonObject().get("count").getAsInt());
    }

    @Test
    void noSaveUpdatesMemoryWithoutWritingFile() {
        ConfigUnit<RuntimeConfig> unit = definition("no_save");
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());

        RuntimeConfig updated = unit.updateNoSave(RuntimeConfig::count, value -> value + 4);

        assertEquals(5, updated.count());
        assertEquals(5, unit.get().count());
        assertFalse(Files.exists(savePath));
    }

    @Test
    void mutationCommitsOnceAndNoSaveVariantSkipsPersistence() {
        ConfigUnit<RuntimeConfig> unit = definition("mutation");
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());
        ChangeCounter counter = new ChangeCounter(unit);
        EventBus.register(counter);
        try {

            RuntimeConfig draft = unit.applyMutationNoSave(unit.mutation()
                    .map(RuntimeConfig::count, value -> value + 2)
                    .set(RuntimeConfig::opt, Optional.of(10))
                    .ifPresent(RuntimeConfig::opt, value -> value + 1));

            assertEquals(new RuntimeConfig(3, Optional.of(11)), draft);
            assertFalse(Files.exists(savePath));
            assertEquals(1, counter.changes.get());

            RuntimeConfig saved = unit.applyMutation(unit.mutation()
                    .map(RuntimeConfig::count, value -> value * 2)
                    .ifPresent(RuntimeConfig::opt, value -> value + 1));

            assertEquals(new RuntimeConfig(6, Optional.of(12)), saved);
            assertTrue(Files.exists(savePath));
            assertEquals(2, counter.changes.get());
        } finally {
            EventBus.unregister(counter);
        }
    }

    @Test
    void failedValidationDoesNotReplaceCurrentValue() {
        ConfigUnit<RuntimeConfig> unit = definition("validation");
        RuntimeConfig original = unit.get();

        assertThrows(IllegalStateException.class,
                () -> unit.update(RuntimeConfig::count, ignored -> 99));
        assertEquals(original, unit.get());
    }

    @Test
    void unchangedCommitDoesNotWriteOrPublishAChange() {
        ConfigUnit<RuntimeConfig> unit = definition("unchanged");
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());
        ChangeCounter counter = new ChangeCounter(unit);
        EventBus.register(counter);
        try {
            RuntimeConfig unchanged = unit.update(RuntimeConfig::count, value -> value);

            assertEquals(new RuntimeConfig(1, Optional.empty()), unchanged);
            assertFalse(Files.exists(savePath));
            assertEquals(0, counter.changes.get());
        } finally {
            EventBus.unregister(counter);
        }
    }

    @Test
    void persistenceFailureLeavesMemoryUntouchedAndPublishesNoChange() throws Exception {
        ConfigUnit<RuntimeConfig> unit = definition("write_failure");
        RuntimeConfig original = unit.get();
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());
        Files.createDirectories(savePath);
        ChangeCounter counter = new ChangeCounter(unit);
        EventBus.register(counter);
        try {
            assertThrows(IllegalStateException.class,
                    () -> unit.update(RuntimeConfig::count, value -> value + 1));

            assertEquals(original, unit.get());
            assertEquals(0, counter.changes.get());
            assertTrue(Files.isDirectory(savePath));
        } finally {
            EventBus.unregister(counter);
        }
    }

    @Test
    void reloadFailureRetainsTheMostRecentlyAcceptedNoSaveValue() throws Exception {
        ConfigUnit<RuntimeConfig> unit = definition("reload_failure");
        RuntimeConfig accepted = unit.updateNoSave(RuntimeConfig::count, value -> value + 3);
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());
        Files.createDirectories(savePath.getParent());
        Files.writeString(savePath, "[]", StandardCharsets.UTF_8);

        ConfigLifecycle.reload(unit);

        assertEquals(accepted, unit.get());
        assertEquals("[]", Files.readString(savePath, StandardCharsets.UTF_8));
    }

    @Test
    void explicitPersistFailureRetainsTheMostRecentlyAcceptedValue() throws Exception {
        ConfigUnit<RuntimeConfig> unit = definition("explicit_save_failure");
        RuntimeConfig accepted = unit.updateNoSave(RuntimeConfig::count, value -> value + 2);
        var savePath = ConfigIOUtil.resolveSavePath(unit.meta());
        Files.createDirectories(savePath);

        assertDoesNotThrow(() -> ConfigLifecycle.persist(unit));

        assertEquals(accepted, unit.get());
        assertTrue(Files.isDirectory(savePath));
    }

    private static ConfigUnit<RuntimeConfig> definition(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        return ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", prefix + "_" + suffix),
                RuntimeConfig.class,
                meta -> meta.fileName(prefix + "_" + suffix)
                        .directory("unit-tests")
                        .format(ConfigStorageFormat.JSON),
                schema -> schema.group(
                        ConfigField.intRange("count", 0, 10)
                                .defaultValue(1)
                                .validate("maximum", value -> value <= 10
                                        ? Optional.empty()
                                        : Optional.of("must not exceed 10"))
                                .forGetter(RuntimeConfig::count),
                        ConfigField.optional("opt", Codec.INT).forGetter(RuntimeConfig::opt)
                ).apply(schema, RuntimeConfig::new)
        );
    }

    private record RuntimeConfig(int count, Optional<Integer> opt) {
    }

    private static final class ChangeCounter {
        private final ConfigUnit<?> expected;
        private final AtomicInteger changes = new AtomicInteger();

        private ChangeCounter(ConfigUnit<?> expected) {
            this.expected = expected;
        }

        @Subscribe
        public void changed(ConfigChangedEvent<?> event) {
            if (event.unit() == expected) {
                changes.incrementAndGet();
            }
        }
    }
}
