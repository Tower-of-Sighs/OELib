package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.optics.*;
import cc.sighs.oelib.config.testsupport.TestFileUtil;
import cc.sighs.oelib.config.testsupport.TestPlatform;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTraversalTest {

    private record Server(String name, int port) {
    }

    private record Cluster(List<Server> servers, Map<String, Integer> ports, String label) {
    }

    private record ListRoot(List<Integer> values, String name) {
    }

    private record MapRoot(Map<String, String> tags, String name) {
    }

    @BeforeEach
    void clean() throws Exception {
        TestFileUtil.cleanDirectory(TestPlatform.CONFIG_PATH);
        Files.createDirectories(TestPlatform.CONFIG_PATH);
    }

    // -- ConfigTraversal.onList --

    @Test
    void listTraversalGetAllReturnsAllElements() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens);

        ListRoot root = new ListRoot(List.of(1, 2, 3), "x");
        assertEquals(List.of(1, 2, 3), traversal.extract(root));
    }

    @Test
    void listTraversalGetAllReturnsEmptyListForEmptyField() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens);

        ListRoot root = new ListRoot(List.of(), "x");
        assertTrue(traversal.extract(root).isEmpty());
    }

    @Test
    void listTraversalUpdateTransformsAllElements() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens);

        ListRoot root = new ListRoot(List.of(1, 2, 3), "x");
        ListRoot updated = traversal.update(root, v -> v * 10);

        assertEquals(List.of(10, 20, 30), updated.values());
        assertEquals("x", updated.name());
    }

    @Test
    void listTraversalUpdateOnEmptyListIsNoOp() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens);

        ListRoot root = new ListRoot(List.of(), "x");
        ListRoot updated = traversal.update(root, v -> v + 1);

        assertTrue(updated.values().isEmpty());
    }

    @Test
    void listTraversalCountReturnsCorrectSize() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens);

        assertEquals(3, traversal.count(new ListRoot(List.of(1, 2, 3), "x")));
        assertEquals(0, traversal.count(new ListRoot(List.of(), "x")));
    }

    @Test
    void listTraversalAnyMatchAndAllMatch() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens);

        ListRoot root = new ListRoot(List.of(2, 4, 6), "x");

        assertTrue(traversal.anyMatch(root, v -> v > 4));
        assertFalse(traversal.anyMatch(root, v -> v < 0));
        assertTrue(traversal.allMatch(root, v -> v % 2 == 0));
        assertFalse(traversal.allMatch(root, v -> v > 3));
    }

    @Test
    void listTraversalAllMatchReturnsTrueForEmptyList() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens);

        assertTrue(traversal.allMatch(new ListRoot(List.of(), "x"), v -> false));
    }

    @Test
    void listTraversalFindFirstReturnsFirstMatch() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens);

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4, 5), "x");

        assertEquals(Optional.of(3), traversal.findFirst(root, v -> v > 2));
        assertEquals(Optional.empty(), traversal.findFirst(root, v -> v > 10));
    }

    // -- ConfigTraversal.onList filter --

    @Test
    void listTraversalFilterSelectivelyUpdatesElements() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens).filter(v -> v % 2 == 0);

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4), "x");
        ListRoot updated = traversal.update(root, v -> v * 100);

        assertEquals(List.of(1, 200, 3, 400), updated.values());
    }

    @Test
    void listTraversalFilterGetAllOnlyReturnsMatching() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens).filter(v -> v > 2);

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4, 5), "x");
        assertEquals(List.of(3, 4, 5), traversal.extract(root));
    }

    // -- ConfigTraversal compose with lens --

    @Test
    void listTraversalComposeWithLensFocusesNestedField() {
        ConfigLens<Cluster, List<Server>> listLens = RecordLensBuilder.lens(Cluster.class, Cluster::servers);
        ConfigLens<Server, Integer> portLens = RecordLensBuilder.lens(Server.class, Server::port);
        ConfigTraversal<Cluster, Integer> portTraversal = Traversals.onList(listLens).compose(portLens);

        Cluster cluster = new Cluster(
                List.of(new Server("a", 80), new Server("b", 443)),
                Map.of(),
                "test"
        );

        List<Integer> ports = portTraversal.extract(cluster);
        assertEquals(List.of(80, 443), ports);

        Cluster updated = portTraversal.update(cluster, p -> p + 1);
        assertEquals(81, updated.servers().getFirst().port());
        assertEquals(444, updated.servers().get(1).port());
    }

    @Test
    void listTraversalToMutationProducesWorkingMutation() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigTraversal<ListRoot, Integer> traversal = Traversals.onList(lens);

        ConfigMutation<ListRoot> mutation = traversal.toMutation(v -> v * 3);
        ListRoot root = new ListRoot(List.of(1, 2, 3), "x");

        ListRoot result = mutation.apply(root);
        assertEquals(List.of(3, 6, 9), result.values());
    }

    // -- ConfigFold --

    @Test
    void foldGetAllReturnsAllElements() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigFold<ListRoot, Integer> fold = Traversals.onList(lens);

        ListRoot root = new ListRoot(List.of(10, 20, 30), "x");
        assertEquals(List.of(10, 20, 30), fold.extract(root));
    }

    @Test
    void foldAggregatesWithAccumulator() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigFold<ListRoot, Integer> fold = Traversals.onList(lens);

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4, 5), "x");
        int sum = fold.fold(root, 0, Integer::sum);

        assertEquals(15, sum);
    }

    @Test
    void foldFiltersCorrectly() {
        ConfigLens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        ConfigFold<ListRoot, Integer> fold = Traversals.onList(lens).filter(v -> v > 2);

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4, 5), "x");
        assertEquals(List.of(3, 4, 5), fold.extract(root));
    }

    // -- ConfigTraversal.onMapValues --

    @Test
    void mapValuesTraversalGetAllReturnsAllValues() {
        ConfigLens<MapRoot, Map<String, String>> lens = RecordLensBuilder.lens(MapRoot.class, MapRoot::tags);
        ConfigTraversal<MapRoot, String> traversal = Traversals.onMapValues(lens);

        MapRoot root = new MapRoot(Map.of("a", "x", "b", "y"), "n");
        assertTrue(traversal.extract(root).containsAll(List.of("x", "y")));
    }

    @Test
    void mapValuesTraversalUpdateTransformsAllValues() {
        ConfigLens<MapRoot, Map<String, String>> lens = RecordLensBuilder.lens(MapRoot.class, MapRoot::tags);
        ConfigTraversal<MapRoot, String> traversal = Traversals.onMapValues(lens);

        MapRoot root = new MapRoot(Map.of("k1", "hello", "k2", "world"), "n");
        MapRoot updated = traversal.update(root, String::toUpperCase);

        assertEquals("HELLO", updated.tags().get("k1"));
        assertEquals("WORLD", updated.tags().get("k2"));
        assertEquals("n", updated.name());
    }

    @Test
    void mapValuesCountReturnsCorrectSize() {
        ConfigLens<MapRoot, Map<String, String>> lens = RecordLensBuilder.lens(MapRoot.class, MapRoot::tags);
        ConfigTraversal<MapRoot, String> traversal = Traversals.onMapValues(lens);

        MapRoot root = new MapRoot(Map.of("a", "1", "b", "2"), "n");
        assertEquals(2, traversal.count(root));
    }

    // -- ConfigFold.onMapKeys --

    @Test
    void mapKeysFoldGetAllReturnsAllKeys() {
        ConfigLens<MapRoot, Map<String, String>> lens = RecordLensBuilder.lens(MapRoot.class, MapRoot::tags);
        ConfigFold<MapRoot, String> fold = Folds.onMapKeys(lens);

        MapRoot root = new MapRoot(Map.of("x", "1", "y", "2"), "n");
        assertTrue(fold.extract(root).containsAll(List.of("x", "y")));
    }

    // -- ConfigUnit semantic API (read-only, no persistence dependency) --

    @Test
    void countReturnsNumberOfListElements() {
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                Identifier.fromNamespaceAndPath("oelibtest", "c_" + UUID.randomUUID().toString().replace("-", "")),
                ListRoot.class,
                meta -> meta.fileName("count_test").directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(List.of(10, 20, 30)).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );

        assertEquals(3, def.unit().count(ListRoot::values));
    }

    @Test
    void anyMatchAndAllMatchReturnCorrectResults() {
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                Identifier.fromNamespaceAndPath("oelibtest", "m_" + UUID.randomUUID().toString().replace("-", "")),
                ListRoot.class,
                meta -> meta.fileName("match_test").directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(List.of(2, 4, 6)).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );

        ConfigUnit<ListRoot> unit = def.unit();
        assertTrue(unit.anyMatch(ListRoot::values, v -> v > 4));
        assertFalse(unit.anyMatch(ListRoot::values, v -> v > 10));
        assertTrue(unit.allMatch(ListRoot::values, v -> v % 2 == 0));
        assertFalse(unit.allMatch(ListRoot::values, v -> v < 6));
    }

    @Test
    void getAllWhereReturnsFilteredElements() {
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                Identifier.fromNamespaceAndPath("oelibtest", "gw_" + UUID.randomUUID().toString().replace("-", "")),
                ListRoot.class,
                meta -> meta.fileName("get_all_where").directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(List.of(1, 2, 3, 4, 5)).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );

        List<Integer> result = def.unit().getAllWhere(ListRoot::values, v -> v > 3);
        assertEquals(List.of(4, 5), result);
    }

    @Test
    void getValuesAndGetKeysReturnCorrectCollections() {
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                Identifier.fromNamespaceAndPath("oelibtest", "kv_" + UUID.randomUUID().toString().replace("-", "")),
                MapRoot.class,
                meta -> meta.fileName("get_keys_vals").directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.map("tags", Codec.STRING, Codec.STRING).defaultValue(Map.of("ka", "va", "kb", "vb")).forGetter(MapRoot::tags),
                        ConfigField.string("name").defaultValue("n").forGetter(MapRoot::name)
                ).apply(schema, MapRoot::new)
        );

        ConfigUnit<MapRoot> unit = def.unit();
        assertTrue(unit.getKeys(MapRoot::tags).containsAll(List.of("ka", "kb")));
        assertTrue(unit.getValues(MapRoot::tags).containsAll(List.of("va", "vb")));
    }

    // -- ConfigUnit semantic API (write, requires isolated filenames) --

    @Test
    void updateElementsTransformsAllListElements() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                Identifier.fromNamespaceAndPath("oelibtest", id),
                ListRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(List.of(2, 4, 6)).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );

        def.unit().updateElements(ListRoot::values, v -> v + 1);
        assertEquals(List.of(3, 5, 7), def.unit().getAll(ListRoot::values));
    }

    @Test
    void updateWhereOnlyTransformsMatchingElements() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                Identifier.fromNamespaceAndPath("oelibtest", id),
                ListRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(List.of(1, 2, 3, 4, 5)).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );

        def.unit().updateWhere(ListRoot::values, v -> v % 2 == 0, v -> v * 100);
        assertEquals(List.of(1, 200, 3, 400, 5), def.unit().getAll(ListRoot::values));
    }

    @Test
    void updateValuesTransformsAllMapValues() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                Identifier.fromNamespaceAndPath("oelibtest", id),
                MapRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.map("tags", Codec.STRING, Codec.STRING).defaultValue(Map.of("a", "x", "b", "y")).forGetter(MapRoot::tags),
                        ConfigField.string("name").defaultValue("n").forGetter(MapRoot::name)
                ).apply(schema, MapRoot::new)
        );

        def.unit().updateValues(MapRoot::tags, String::toUpperCase);
        assertEquals("X", def.unit().get().tags().get("a"));
        assertEquals("Y", def.unit().get().tags().get("b"));
    }

    // -- ConfigUnitOps --

    @Test
    void traverseNoSaveDoesNotPersist() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                Identifier.fromNamespaceAndPath("oelibtest", id),
                ListRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(List.of(1, 2, 3)).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );

        ConfigUnit<ListRoot> unit = def.unit();
        var lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        var traversal = Traversals.onList(lens);

        unit.get();
        ConfigUnitOps.traverseNoSave(unit, traversal, v -> v * 10);

        assertEquals(List.of(10, 20, 30), unit.getAll(ListRoot::values));

        Path savePath = Path.of("build", "test-config", "unit-tests", id + ".json5");
        assertFalse(Files.exists(savePath));
    }

    @Test
    void batchMutatorTraverseAppliesTransformation() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                Identifier.fromNamespaceAndPath("oelibtest", id),
                ListRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(List.of(1, 2, 3)).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );

        ConfigUnit<ListRoot> unit = def.unit();
        var lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        var traversal = Traversals.onList(lens);

        unit.get();
        ConfigUnitOps.withBatchNoSave(unit, batch -> batch.traverse(traversal, v -> v * 10));

        assertEquals(List.of(10, 20, 30), unit.getAll(ListRoot::values));
    }
}
