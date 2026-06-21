package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.testsupport.TestFileUtil;
import cc.sighs.oelib.config.testsupport.TestPlatform;
import com.flechazo.optics.Each;
import com.flechazo.optics.Fold;
import com.flechazo.optics.Lens;
import com.flechazo.optics.Traversal;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
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
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal());

        ListRoot root = new ListRoot(List.of(1, 2, 3), "x");
        assertEquals(List.of(1, 2, 3), traversal.getAll(root));
    }

    @Test
    void listTraversalGetAllReturnsEmptyListForEmptyField() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal());

        ListRoot root = new ListRoot(List.of(), "x");
        assertTrue(traversal.getAll(root).isEmpty());
    }

    @Test
    void listTraversalUpdateTransformsAllElements() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal());

        ListRoot root = new ListRoot(List.of(1, 2, 3), "x");
        ListRoot updated = traversal.modify(v -> v * 10, root);

        assertEquals(List.of(10, 20, 30), updated.values());
        assertEquals("x", updated.name());
    }

    @Test
    void listTraversalUpdateOnEmptyListIsNoOp() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal());

        ListRoot root = new ListRoot(List.of(), "x");
        ListRoot updated = traversal.modify(v -> v + 1, root);

        assertTrue(updated.values().isEmpty());
    }

    @Test
    void listTraversalCountReturnsCorrectSize() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal());

        assertEquals(3, traversal.length(new ListRoot(List.of(1, 2, 3), "x")));
        assertEquals(0, traversal.length(new ListRoot(List.of(), "x")));
    }

    @Test
    void listTraversalAnyMatchAndAllMatch() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal());

        ListRoot root = new ListRoot(List.of(2, 4, 6), "x");

        assertTrue(traversal.exists(v -> v > 4, root));
        assertFalse(traversal.exists(v -> v < 0, root));
        assertTrue(traversal.all(v -> v % 2 == 0, root));
        assertFalse(traversal.all(v -> v > 3, root));
    }

    @Test
    void listTraversalAllMatchReturnsTrueForEmptyList() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal());

        assertTrue(traversal.all(v -> false, new ListRoot(List.of(), "x")));
    }

    @Test
    void listTraversalFindFirstReturnsFirstMatch() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal());

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4, 5), "x");

        assertEquals(Optional.of(3), traversal.asFold().findOptional(v -> v > 2, root));
        assertEquals(Optional.empty(), traversal.asFold().findOptional(v -> v > 10, root));
    }

    // -- ConfigTraversal.onList filter --

    @Test
    void listTraversalFilterSelectivelyUpdatesElements() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal()).filtered(v -> v % 2 == 0);

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4), "x");
        ListRoot updated = traversal.modify(v -> v * 100, root);

        assertEquals(List.of(1, 200, 3, 400), updated.values());
    }

    @Test
    void listTraversalFilterGetAllOnlyReturnsMatching() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal()).filtered(v -> v > 2);

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4, 5), "x");
        assertEquals(List.of(3, 4, 5), traversal.getAll(root));
    }

    // -- ConfigTraversal compose with lens --

    @Test
    void listTraversalComposeWithLensFocusesNestedField() {
        Lens<Cluster, List<Server>> listLens = RecordLensBuilder.lens(Cluster.class, Cluster::servers);
        Lens<Server, Integer> portLens = RecordLensBuilder.lens(Server.class, Server::port);
        Traversal<Cluster, Integer> portTraversal = listLens.andThen(Each.listTraversal()).andThen(portLens);

        Cluster cluster = new Cluster(
                List.of(new Server("a", 80), new Server("b", 443)),
                Map.of(),
                "test"
        );

        List<Integer> ports = portTraversal.getAll(cluster);
        assertEquals(List.of(80, 443), ports);

        Cluster updated = portTraversal.modify(p -> p + 1, cluster);
        assertEquals(81, updated.servers().get(0).port());
        assertEquals(444, updated.servers().get(1).port());
    }

    @Test
    void listTraversalToMutationProducesWorkingMutation() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Traversal<ListRoot, Integer> traversal = lens.andThen(Each.listTraversal());

        ConfigMutation<ListRoot> mutation = source -> traversal.modify(v -> v * 3, source);
        ListRoot root = new ListRoot(List.of(1, 2, 3), "x");

        ListRoot result = mutation.apply(root);
        assertEquals(List.of(3, 6, 9), result.values());
    }

    // -- ConfigFold --

    @Test
    void foldGetAllReturnsAllElements() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Fold<ListRoot, Integer> fold = lens.andThen(Each.listTraversal()).asFold();

        ListRoot root = new ListRoot(List.of(10, 20, 30), "x");
        assertEquals(List.of(10, 20, 30), fold.getAll(root));
    }

    @Test
    void foldAggregatesWithAccumulator() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Fold<ListRoot, Integer> fold = lens.andThen(Each.listTraversal()).asFold();

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4, 5), "x");
        int sum = fold.getAll(root).stream().reduce(0, Integer::sum);

        assertEquals(15, sum);
    }

    @Test
    void foldFiltersCorrectly() {
        Lens<ListRoot, List<Integer>> lens = RecordLensBuilder.lens(ListRoot.class, ListRoot::values);
        Fold<ListRoot, Integer> fold = lens.andThen(Each.listTraversal()).filtered(v -> v > 2).asFold();

        ListRoot root = new ListRoot(List.of(1, 2, 3, 4, 5), "x");
        assertEquals(List.of(3, 4, 5), fold.getAll(root));
    }

    // -- ConfigTraversal.onMapValues --

    @Test
    void mapValuesTraversalGetAllReturnsAllValues() {
        Lens<MapRoot, Map<String, String>> lens = RecordLensBuilder.lens(MapRoot.class, MapRoot::tags);
        Traversal<MapRoot, String> traversal = lens.andThen(Traversal.mapValues());

        MapRoot root = new MapRoot(Map.of("a", "x", "b", "y"), "n");
        assertTrue(traversal.getAll(root).containsAll(List.of("x", "y")));
    }

    @Test
    void mapValuesTraversalUpdateTransformsAllValues() {
        Lens<MapRoot, Map<String, String>> lens = RecordLensBuilder.lens(MapRoot.class, MapRoot::tags);
        Traversal<MapRoot, String> traversal = lens.andThen(Traversal.mapValues());

        MapRoot root = new MapRoot(Map.of("k1", "hello", "k2", "world"), "n");
        MapRoot updated = traversal.modify(String::toUpperCase, root);

        assertEquals("HELLO", updated.tags().get("k1"));
        assertEquals("WORLD", updated.tags().get("k2"));
        assertEquals("n", updated.name());
    }

    @Test
    void mapValuesCountReturnsCorrectSize() {
        Lens<MapRoot, Map<String, String>> lens = RecordLensBuilder.lens(MapRoot.class, MapRoot::tags);
        Traversal<MapRoot, String> traversal = lens.andThen(Traversal.mapValues());

        MapRoot root = new MapRoot(Map.of("a", "1", "b", "2"), "n");
        assertEquals(2, traversal.length(root));
    }

    // -- ConfigFold.onMapKeys --

    @Test
    void mapKeysFoldGetAllReturnsAllKeys() {
        Lens<MapRoot, Map<String, String>> lens = RecordLensBuilder.lens(MapRoot.class, MapRoot::tags);
        Fold<MapRoot, String> fold = lens.andThen(Fold.mapKeys());

        MapRoot root = new MapRoot(Map.of("x", "1", "y", "2"), "n");
        assertTrue(fold.getAll(root).containsAll(List.of("x", "y")));
    }

    // -- ConfigUnit semantic API (read-only, no persistence dependency) --

    @Test
    void countReturnsNumberOfListElements() {
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", "c_" + UUID.randomUUID().toString().replace("-", "")),
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
                ResourceLocation.fromNamespaceAndPath("oelibtest", "m_" + UUID.randomUUID().toString().replace("-", "")),
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
                ResourceLocation.fromNamespaceAndPath("oelibtest", "gw_" + UUID.randomUUID().toString().replace("-", "")),
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
                ResourceLocation.fromNamespaceAndPath("oelibtest", "kv_" + UUID.randomUUID().toString().replace("-", "")),
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
        assertEquals(2, unit.countKeys(MapRoot::tags));
        assertEquals(2, unit.countValues(MapRoot::tags));
        assertTrue(unit.anyKeyMatch(MapRoot::tags, key -> key.equals("ka")));
        assertTrue(unit.allKeyMatch(MapRoot::tags, key -> key.startsWith("k")));
        assertEquals(Optional.of("kb"), unit.findKey(MapRoot::tags, key -> key.endsWith("b")));
        assertTrue(unit.anyValueMatch(MapRoot::tags, value -> value.equals("va")));
        assertTrue(unit.allValueMatch(MapRoot::tags, value -> value.startsWith("v")));
        assertEquals(Optional.of("vb"), unit.findValue(MapRoot::tags, value -> value.endsWith("b")));
        assertEquals(List.of("vb"), unit.getValuesWhere(MapRoot::tags, value -> value.endsWith("b")));
    }

    // -- ConfigUnit semantic API (write, requires isolated filenames) --

    @Test
    void updateElementsTransformsAllListElements() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
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
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
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
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
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

        def.unit().updateValuesWhere(MapRoot::tags, value -> value.equals("X"), value -> value + "!");
        assertEquals("X!", def.unit().get().tags().get("a"));
        assertEquals("Y", def.unit().get().tags().get("b"));
    }

    @Test
    void configMutationUpdateValuesWhereSupportsGetterAndPath() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
                MapRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.map("tags", Codec.STRING, Codec.STRING).defaultValue(Map.of("a", "x", "b", "y")).forGetter(MapRoot::tags),
                        ConfigField.string("name").defaultValue("n").forGetter(MapRoot::name)
                ).apply(schema, MapRoot::new)
        );

        ConfigUnit<MapRoot> unit = def.unit();
        ConfigPath.Many<MapRoot, String> values = def.pathValues(MapRoot::tags);

        unit.updateAll(
                ConfigMutation.updateValuesWhere(MapRoot::tags, value -> value.equals("x"), String::toUpperCase),
                ConfigMutation.paths().updateWhere(values, value -> value.equals("y"), String::toUpperCase)
        );

        assertEquals("X", unit.get().tags().get("a"));
        assertEquals("Y", unit.get().tags().get("b"));
    }

    // -- ConfigUnitOps --

    @Test
    void updateValuesWhereNoSaveOnlyTransformsMatchingMapValues() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
                MapRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.map("tags", Codec.STRING, Codec.STRING).defaultValue(Map.of("a", "x", "b", "y")).forGetter(MapRoot::tags),
                        ConfigField.string("name").defaultValue("n").forGetter(MapRoot::name)
                ).apply(schema, MapRoot::new)
        );

        ConfigUnit<MapRoot> unit = def.unit();
        unit.get();
        ConfigUnitOps.updateValuesWhereNoSave(unit, MapRoot::tags, value -> value.equals("x"), String::toUpperCase);

        assertEquals("X", unit.get().tags().get("a"));
        assertEquals("y", unit.get().tags().get("b"));
        Path savePath = Path.of("build", "test-config", "unit-tests", id + ".json5");
        assertFalse(Files.exists(savePath));
    }

    @Test
    void updateValuesWhereNoSaveAcceptsPath() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
                MapRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.map("tags", Codec.STRING, Codec.STRING).defaultValue(Map.of("a", "x", "b", "y")).forGetter(MapRoot::tags),
                        ConfigField.string("name").defaultValue("n").forGetter(MapRoot::name)
                ).apply(schema, MapRoot::new)
        );

        ConfigUnit<MapRoot> unit = def.unit();
        ConfigPath.Many<MapRoot, String> values = def.pathValues(MapRoot::tags);

        unit.get();
        ConfigUnitOps.paths(unit).updateWhereNoSave(values, value -> value.equals("y"), String::toUpperCase);

        assertEquals("x", unit.get().tags().get("a"));
        assertEquals("Y", unit.get().tags().get("b"));
    }

    @Test
    void updateElementsNoSaveDoesNotPersist() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
                ListRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(List.of(1, 2, 3)).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );

        ConfigUnit<ListRoot> unit = def.unit();

        unit.get();
        ConfigUnitOps.updateElementsNoSave(unit, ListRoot::values, v -> v * 10);

        assertEquals(List.of(10, 20, 30), unit.getAll(ListRoot::values));

        Path savePath = Path.of("build", "test-config", "unit-tests", id + ".json5");
        assertFalse(Files.exists(savePath));
    }

    @Test
    void batchMutatorUpdateElementsAppliesTransformation() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
                ListRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.list("values", Codec.INT).defaultValue(List.of(1, 2, 3)).forGetter(ListRoot::values),
                        ConfigField.string("name").defaultValue("n").forGetter(ListRoot::name)
                ).apply(schema, ListRoot::new)
        );

        ConfigUnit<ListRoot> unit = def.unit();

        unit.get();
        ConfigUnitOps.withBatchNoSave(unit, batch -> batch.updateElements(ListRoot::values, v -> v * 10));

        assertEquals(List.of(10, 20, 30), unit.getAll(ListRoot::values));
    }

    @Test
    void batchMutatorUpdateValuesWhereAppliesTransformation() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
                MapRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.map("tags", Codec.STRING, Codec.STRING).defaultValue(Map.of("a", "x", "b", "y")).forGetter(MapRoot::tags),
                        ConfigField.string("name").defaultValue("n").forGetter(MapRoot::name)
                ).apply(schema, MapRoot::new)
        );

        ConfigUnit<MapRoot> unit = def.unit();

        unit.get();
        ConfigUnitOps.withBatchNoSave(unit, batch -> batch.updateValuesWhere(MapRoot::tags, value -> value.equals("y"), String::toUpperCase));

        assertEquals("x", unit.get().tags().get("a"));
        assertEquals("Y", unit.get().tags().get("b"));
    }

    @Test
    void batchMutatorUpdateValuesWhereAcceptsPath() {
        String id = UUID.randomUUID().toString().replace("-", "");
        var def = ConfigSchema.defineClient(
                MethodHandles.lookup(),
                ResourceLocation.fromNamespaceAndPath("oelibtest", id),
                MapRoot.class,
                meta -> meta.fileName(id).directory("unit-tests"),
                schema -> schema.group(
                        ConfigField.map("tags", Codec.STRING, Codec.STRING).defaultValue(Map.of("a", "x", "b", "y")).forGetter(MapRoot::tags),
                        ConfigField.string("name").defaultValue("n").forGetter(MapRoot::name)
                ).apply(schema, MapRoot::new)
        );

        ConfigUnit<MapRoot> unit = def.unit();
        ConfigPath.Many<MapRoot, String> values = def.pathValues(MapRoot::tags);

        unit.get();
        ConfigUnitOps.withBatchNoSave(unit, batch -> batch.paths().updateWhere(values, value -> value.equals("x"), String::toUpperCase));

        assertEquals("X", unit.get().tags().get("a"));
        assertEquals("y", unit.get().tags().get("b"));
    }
}
