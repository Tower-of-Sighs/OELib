package cc.sighs.oelib.config.util;

import cc.sighs.oelib.config.ConfigContext;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import net.minecraft.resources.ResourceLocation;
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
     * id and field key, qualified by the active
     * {@link ConfigContext}.
     *
     * <p>The resulting key follows the pattern
     * {@code "config.<namespace>.<path>.<qualifiedFieldKey>"}.
     *
     * @param configId the configuration id
     * @param key      the local field key
     * @return the translation key, or {@code null} if {@code configId} is {@code null}
     */
    @Nullable
    public static String autoTranslationKey(ResourceLocation configId, String key) {
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
                .visibleWhenPath(meta.visibleWhenPath().map(ConfigContext::qualifyKey).orElse(null))
                .visibleWhenValue(meta.visibleWhenValue().orElse(null))
                .defaultJsonValue(meta.defaultJsonValue().orElse(null))
                .validators(meta.validators())
                .migrations(meta.migrations())
                .build();
    }

    /**
     * Rewrites nested metadata for the current context and the given
     * configuration id.
     *
     * @param  meta the nested metadata entry
     * @param  configId the target configuration id, or {@code null}
     * @return the rewritten metadata entry
     */
    public static ConfigValueMeta rewriteNestedMetaForContext(ConfigValueMeta meta, @Nullable ResourceLocation configId) {
        ConfigValueMeta.Builder builder = ConfigValueMeta.builder(meta.key())
                .comment(meta.comment().orElse(null))
                .uiHint(meta.uiHint().orElse(null))
                .hidden(meta.hidden())
                .visibleWhenPath(meta.visibleWhenPath().orElse(null))
                .visibleWhenValue(meta.visibleWhenValue().orElse(null))
                .defaultJsonValue(meta.defaultJsonValue().orElse(null))
                .validators(meta.validators())
                .migrations(meta.migrations());

        if (configId != null) {
            String autoKey = autoTranslationKey(configId, meta.key());
            builder.translationKey(autoKey);
            if (meta.tooltip().isPresent()) {
                builder.tooltip(autoKey + ".tooltip");
            }
        } else {
            builder.translationKey(meta.translationKey().orElse(null));
            builder.tooltip(meta.tooltip().orElse(null));
        }

        return qualifyForContext(builder.build());
    }
}
