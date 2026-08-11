package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import com.flechazo.optics.Fold;
import com.flechazo.optics.Lens;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.util.Traversals;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTraversalTest {

    @Test
    void nativeListTraversalRebuildsWithoutMutatingSource() {
        Traversal<List<Integer>, Integer> traversal = Traversals.forList();
        List<Integer> source = List.of(1, 2, 3);

        List<Integer> updated = traversal.modify(value -> value * 2, source);

        assertEquals(List.of(2, 4, 6), updated);
        assertEquals(List.of(1, 2, 3), source);
    }

    @Test
    void nativeMapValueTraversalPreservesKeys() {
        Traversal<Map<String, Integer>, Integer> traversal = Traversals.forMapValues();
        Map<String, Integer> source = Map.of("a", 1, "b", 2);

        Map<String, Integer> updated = traversal.modify(value -> value + 10, source);

        assertEquals(11, updated.get("a"));
        assertEquals(12, updated.get("b"));
        assertEquals(Map.of("a", 1, "b", 2), source);
    }

    @Test
    void nativeFoldQueriesMapKeys() {
        Lens<MapRoot, Map<String, String>> lens = Lens.of(MapRoot.class, MapRoot::tags);
        Fold<MapRoot, String> keys = lens.andThen(Fold.mapKeys());
        MapRoot source = new MapRoot(Map.of("x", "1", "y", "2"), "n");

        assertTrue(keys.getAll(source).containsAll(List.of("x", "y")));
        assertTrue(keys.exists(key -> key.equals("x"), source));
    }

    @Test
    void directTraversalApiKeepsDescriptiveOperationNames() {
        ConfigUnit<ListRoot> unit = listDefinition(List.of(1, 2, 3, 4));

        unit.updateWhereNoSave(ListRoot::values, value -> value % 2 == 0, value -> value * 10);

        assertEquals(List.of(1, 20, 3, 40), unit.getAll(ListRoot::values));
        assertEquals(4, unit.count(ListRoot::values));
        assertTrue(unit.anyMatch(ListRoot::values, value -> value > 30));
        assertTrue(unit.allMatch(ListRoot::values, value -> value > 0));
    }

    @Test
    void directMapApiSupportsNoSaveAndQueries() {
        ConfigUnit<MapRoot> unit = mapDefinition(Map.of("a", "x", "b", "y"));

        unit.updateValuesWhereNoSave(
                MapRoot::tags, value -> value.equals("y"), String::toUpperCase);

        assertEquals("x", unit.get().tags().get("a"));
        assertEquals("Y", unit.get().tags().get("b"));
        assertTrue(unit.getKeys(MapRoot::tags).containsAll(List.of("a", "b")));
        assertEquals(2, unit.countValues(MapRoot::tags));
    }

    private static ConfigUnit<ListRoot> listDefinition(List<Integer> values) {
        String id = UUID.randomUUID().toString().replace("-", "");
        return ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", id),
                ListRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(values).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );
    }

    private static ConfigUnit<MapRoot> mapDefinition(Map<String, String> values) {
        String id = UUID.randomUUID().toString().replace("-", "");
        return ConfigSchema.defineClient(
                MethodHandles.lookup(),
                new ResourceLocation("oelibtest", id),
                MapRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.map("tags", Codec.STRING, Codec.STRING)
                                .defaultValue(values).forGetter(MapRoot::tags),
                        ConfigField.string("name").defaultValue("n").forGetter(MapRoot::name)
                ).apply(schema, MapRoot::new)
        );
    }

    private record ListRoot(List<Integer> values, String name) {
    }

    private record MapRoot(Map<String, String> tags, String name) {
    }
}
