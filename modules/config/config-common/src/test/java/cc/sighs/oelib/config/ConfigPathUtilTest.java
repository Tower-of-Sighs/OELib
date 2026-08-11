package cc.sighs.oelib.config;

import cc.sighs.oelib.config.util.ConfigPathUtil;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

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
    void removeMissingJsonPathIsNoop() {
        JsonObject root = new JsonObject();
        root.addProperty("x", 1);
        ConfigPathUtil.removeJsonByPath(root, "a.b.c");
        assertTrue(root.has("x"));
    }

}
