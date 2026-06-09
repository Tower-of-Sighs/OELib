package cc.sighs.oelib.config;

import cc.sighs.oelib.config.codecs.ConfigMetaCodec;
import cc.sighs.oelib.config.field.ConfigField;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPathApiTest {

    @Test
    void nestedOnePathUpdatesWithoutManualLens() {
        var definition = definition();
        ConfigUnit<PathRoot> unit = definition.unit();
        ConfigPath.One<PathRoot, Integer> path = definition.path(PathRoot::nested).then(Child::count);

        unit.setValue(withOptionalChild(unit.get(), new Child(1, true)));
        PathRoot updated = ConfigUnitOps.updateNoSave(unit, path, value -> value + 1);

        assertEquals(2, updated.nested().count());
        assertEquals(1, updated.optionalChild().orElseThrow().count());
    }

    @Test
    void maybePathUpdatesOptionalChildWhenPresent() {
        var definition = definition();
        ConfigUnit<PathRoot> unit = definition.unit();
        ConfigPath.Maybe<PathRoot, Boolean> path = definition.pathOptional(PathRoot::optionalChild).then(Child::enabled);

        unit.setValue(withOptionalChild(unit.get(), new Child(1, true)));
        PathRoot updated = ConfigUnitOps.ifPresentNoSave(unit, path, value -> !value);

        assertFalse(updated.optionalChild().orElseThrow().enabled());
        assertTrue(updated.nested().enabled());
    }

    @Test
    void manyPathSelectsSpecificListElementByIndex() {
        var definition = definition();
        ConfigUnit<PathRoot> unit = definition.unit();
        ConfigPath.Maybe<PathRoot, Integer> path = definition.pathEach(PathRoot::entries).then(Entry::weight).at(1);

        unit.get();
        PathRoot updated = ConfigUnitOps.ifPresentNoSave(unit, path, value -> value + 7);

        assertEquals(List.of(
                new Entry("a", 10, Optional.of(new Child(3, true))),
                new Entry("b", 27, Optional.empty())
        ), updated.entries());
    }

    @Test
    void manyPathTraversesOptionalChildrenInsideList() {
        var definition = definition();
        ConfigUnit<PathRoot> unit = definition.unit();
        ConfigPath.Many<PathRoot, Integer> path = definition.pathEach(PathRoot::entries)
                .thenOptional(Entry::child)
                .then(Child::count);

        unit.get();
        PathRoot updated = ConfigUnitOps.updateEachNoSave(unit, path, value -> value + 2);

        assertEquals(5, updated.entries().get(0).child().orElseThrow().count());
        assertTrue(updated.entries().get(1).child().isEmpty());
    }

    @Test
    void pathMutationUpdatesMapValuesAndSupportsQueries() {
        var definition = definition();
        ConfigUnit<PathRoot> unit = definition.unit();
        ConfigPath.Many<PathRoot, Integer> weights = definition.pathValues(PathRoot::entryMap).then(Entry::weight);
        ConfigPath.Many<PathRoot, String> keys = definition.pathKeys(PathRoot::entryMap);

        unit.get();
        PathRoot updated = ConfigUnitOps.updateAllNoSave(unit, ConfigMutation.updateEach(weights, value -> value + 5));

        assertEquals(List.of(15, 25), weights.getAll(updated).stream().sorted().toList());
        assertEquals(List.of("left", "right"), keys.getAll(updated).stream().sorted().toList());
        assertEquals(2, unit.count(weights));
        assertTrue(unit.anyMatch(weights, value -> value >= 20));
        assertEquals(Optional.of(25), unit.findFirst(weights, value -> value >= 20));
    }

    @Test
    void maybePathSelectsSpecificMapValueByKey() {
        var definition = definition();
        ConfigUnit<PathRoot> unit = definition.unit();
        ConfigPath.Maybe<PathRoot, Boolean> path = definition.pathValue(PathRoot::entryMap, "right")
                .thenOptional(Entry::child)
                .then(Child::enabled);

        unit.get();
        PathRoot updated = ConfigUnitOps.ifPresentNoSave(unit, path, value -> !value);

        assertFalse(updated.entryMap().get("right").child().orElseThrow().enabled());
        assertEquals(10, updated.entryMap().get("left").weight());
    }

    private static ConfigSchema.Definition<PathRoot> definition() {
        ConfigMetaCodec<Child> childCodec = ConfigSchema.metaCodec(
                Child.class,
                schema -> schema.group(
                        ConfigField.intRange("count", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(1).forGetter(Child::count),
                        ConfigField.bool("enabled").defaultValue(true).forGetter(Child::enabled)
                ).apply(schema, Child::new)
        );
        ConfigMetaCodec<Entry> entryCodec = ConfigSchema.metaCodec(
                Entry.class,
                schema -> schema.group(
                        ConfigField.string("id").defaultValue("x").forGetter(Entry::id),
                        ConfigField.intRange("weight", Integer.MIN_VALUE, Integer.MAX_VALUE).defaultValue(0).forGetter(Entry::weight),
                        ConfigField.optional("child", childCodec).forGetter(Entry::child)
                ).apply(schema, Entry::new)
        );
        return ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", "config_path_api"),
                PathRoot.class,
                meta -> meta.fileName("config_path_api_test_" + UUID.randomUUID().toString().replace("-", "")).directory("unit-tests"),
                schema -> schema.group(
                        ConfigSchema.record("nested", Child.class, childCodec, PathRoot::nested),
                        ConfigField.optional("optionalChild", childCodec).forGetter(PathRoot::optionalChild),
                        ConfigField.list("entries", entryCodec).defaultValue(List.of(
                                new Entry("a", 10, Optional.of(new Child(3, true))),
                                new Entry("b", 20, Optional.empty())
                        )).forGetter(PathRoot::entries),
                        ConfigField.map("entryMap", Codec.STRING, entryCodec).defaultValue(Map.of(
                                "left", new Entry("left", 10, Optional.empty()),
                                "right", new Entry("right", 20, Optional.of(new Child(4, true)))
                        )).forGetter(PathRoot::entryMap)
                ).apply(schema, PathRoot::new)
        );
    }

    private static PathRoot withOptionalChild(PathRoot source, Child child) {
        return new PathRoot(source.nested(), Optional.of(child), source.entries(), source.entryMap());
    }

    private record Child(int count, boolean enabled) {
    }

    private record Entry(String id, int weight, Optional<Child> child) {
    }

    private record PathRoot(
            Child nested,
            Optional<Child> optionalChild,
            List<Entry> entries,
            Map<String, Entry> entryMap
    ) {
    }
}
