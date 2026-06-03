package cc.sighs.oelib.config.util;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

/**
 * Codec utilities for the configuration framework.
 */
public final class ConfigCodecUtil {
    private ConfigCodecUtil() {
    }

    /**
     * Derives a default value from a codec by parsing an empty JSON object.
     *
     * <p>This requires every field in the codec to specify a default via
     * {@code orElse}, otherwise an {@link IllegalStateException} is thrown.
     *
     * @param codec the codec to derive from
     * @param <T>   the value type
     * @return the derived default value
     * @throws IllegalStateException if the codec lacks defaults for any field
     */
    public static <T> T deriveDefault(Codec<T> codec) {
        var element = new JsonObject();
        var res = codec.parse(JsonOps.INSTANCE, element);
        return res.result()
                .orElseThrow(() -> new IllegalStateException("Missing defaults for config; please specify defaultValue for all fields"));
    }
}
