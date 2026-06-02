package cc.sighs.oelib.dev;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevConfigSchemaTest {

    @Test
    void nestedRecordFieldsUsePrefixedKeysAndTranslationKeys() {
        Map<String, ConfigValueMeta> byKey = DevConfig.UNIT.codec().fields().stream()
                .collect(Collectors.toMap(ConfigValueMeta::key, meta -> meta));

        assertTrue(byKey.containsKey("nestedDemo.enabled"));
        assertTrue(byKey.containsKey("nestedDemo.level"));

        assertEquals(
                "config.oelib.dev_features.nestedDemo.enabled",
                byKey.get("nestedDemo.enabled").translationKey().orElseThrow()
        );
        assertEquals(
                "config.oelib.dev_features.nestedDemo.level",
                byKey.get("nestedDemo.level").translationKey().orElseThrow()
        );
    }
}

