package cc.sighs.oelib.config;

import cc.sighs.oelib.config.util.ConfigPathUtil;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPathUtilTest {

    @Test
    void jsonPathSetGetRemoveWorks() {
        JsonObject root = new JsonObject();
        ConfigPathUtil.setJsonByPath(root, "a.b.c", com.google.gson.JsonParser.parseString("12"));

        assertEquals(12, ConfigPathUtil.getJsonByPath(root, "a.b.c").getAsInt());
        ConfigPathUtil.removeJsonByPath(root, "a.b.c");
        assertNull(ConfigPathUtil.getJsonByPath(root, "a.b.c"));
    }

    @Test
    void objectPathReadsRecordAndMap() {
        TestRoot root = new TestRoot(new TestChild(9), Map.of("k", "v"));
        assertEquals(9, ConfigPathUtil.getObjectByPath(root, "child.value"));
        assertEquals("v", ConfigPathUtil.getObjectByPath(root, "tags.k"));
        assertNull(ConfigPathUtil.getObjectByPath(root, "child.missing"));
    }

    @Test
    void removeMissingJsonPathIsNoop() {
        JsonObject root = new JsonObject();
        root.addProperty("x", 1);
        ConfigPathUtil.removeJsonByPath(root, "a.b.c");
        assertTrue(root.has("x"));
    }

    private record TestRoot(TestChild child, Map<String, String> tags) {
    }

    private record TestChild(int value) {
    }
}
