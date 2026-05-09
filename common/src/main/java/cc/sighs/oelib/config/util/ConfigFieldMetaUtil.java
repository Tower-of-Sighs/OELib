package cc.sighs.oelib.config.util;

import cc.sighs.oelib.config.ConfigContext;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers for deriving translation keys and qualifying field metadata
 * during schema definition.
 */
public final class ConfigFieldMetaUtil {
    private ConfigFieldMetaUtil() {
    }

    /**
     * Derives an automatic translation key from the given configuration
     * identifier and field key, qualified by the active
     * {@link ConfigContext}.
     *
     * <p>The resulting key follows the pattern
     * {@code "config.<namespace>.<path>.<qualifiedFieldKey>"}.
     *
     * @param configId the configuration identifier
     * @param key      the local field key
     * @return the translation key, or {@code null} if {@code configId} is {@code null}
     */
    @Nullable
    public static String autoTranslationKey(Identifier configId, String key) {
        if (configId == null) {
            return null;
        }
        String fullKey = ConfigContext.qualifyKey(key);
        return "config." + configId.getNamespace() + "." + configId.getPath() + "." + fullKey;
    }

    /**
     * Qualifies the key of the given metadata with the current
     * {@link ConfigContext} path prefix, if a context is active.
     *
     * @param meta the field metadata
     * @return the metadata with a qualified key, or the original if no context is active
     */
    public static ConfigValueMeta qualifyForContext(ConfigValueMeta meta) {
        if (!ConfigContext.isActive()) {
            return meta;
        }
        return ConfigValueMeta.builder(ConfigContext.qualifyKey(meta.key()))
                .comment(meta.comment().orElse(null))
                .uiHint(meta.uiHint().orElse(null))
                .translationKey(meta.translationKey().orElse(null))
                .tooltip(meta.tooltip().orElse(null))
                .hidden(meta.hidden())
                .validators(meta.validators())
                .migrations(meta.migrations())
                .build();
    }
}
