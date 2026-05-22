package cc.sighs.oelib.config.serialization;

import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.electronwill.nightconfig.core.CommentedConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between the NightConfig {@link CommentedConfig} tree and the
 * generic {@code Map<String, Object>} object tree used by {@link TomlOps}.
 *
 * <p>This adapter also injects TOML comments from {@link ConfigValueMeta}
 * entries during serialization, using the fully-qualified dotted key to
 * match metadata to tree nodes.
 */
public final class TomlTreeAdapter {
    private TomlTreeAdapter() {
    }

    /**
     * Reads a NightConfig tree into a generic map-based object tree.
     *
     * @param config the NightConfig root
     * @return a generic map tree suitable for {@link TomlOps}
     */
    public static Object readTree(CommentedConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (CommentedConfig.Entry entry : config.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (value instanceof CommentedConfig child) {
                result.put(key, readTree(child));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Writes a generic object tree into a NightConfig tree, attaching
     * comments from the given field metadata.
     *
     * @param config the NightConfig root to populate
     * @param tree   the generic object tree
     * @param fields field metadata used to inject comments
     */
    public static void writeTree(CommentedConfig config, Object tree, Iterable<ConfigValueMeta> fields) {
        if (tree instanceof Map<?, ?> map) {
            writeMap(config, map, "", fields);
        }
    }

    private static void writeMap(CommentedConfig config, Map<?, ?> map, String fullPrefix, Iterable<ConfigValueMeta> fields) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            var key = String.valueOf(entry.getKey());
            var value = entry.getValue();
            var localPath = key;
            var fullPath = fullPrefix.isEmpty() ? key : fullPrefix + "." + key;
            if (value instanceof Map<?, ?> childMap) {
                CommentedConfig child = config.get(localPath);
                if (child == null) {
                    child = config.createSubConfig();
                    config.set(localPath, child);
                }
                writeMap(child, childMap, fullPath, fields);
            } else if (value instanceof List<?> list) {
                config.set(localPath, list);
            } else {
                config.set(localPath, value);
            }
            for (ConfigValueMeta meta : fields) {
                if (meta.key().equals(fullPath) && meta.comment().isPresent()) {
                    config.setComment(localPath, meta.comment().get());
                    break;
                }
            }
        }
    }
}
