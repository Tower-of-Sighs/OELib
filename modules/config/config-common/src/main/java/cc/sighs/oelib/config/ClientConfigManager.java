package cc.sighs.oelib.config;

import cc.sighs.oelib.config.model.ConfigSide;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for client-side configuration units.
 *
 * <p>This manager stores all configurations whose {@link ConfigSide} is
 * {@link ConfigSide#CLIENT}. It reloads configs when the Minecraft resource
 * manager triggers a reload, which covers resource-pack switches and
 * locale changes.
 *
 * <p>The public-facing {@link ConfigManager} delegates client-related
 * operations to this class. Direct use is rarely needed outside the
 * framework internals.
 */
public class ClientConfigManager implements ResourceManagerReloadListener {
    private static final Map<ResourceLocation, ConfigUnit<?>> CONFIGS = new ConcurrentHashMap<>();

    static void registerUnit(ConfigUnit<?> unit) {
        var meta = unit.meta();
        if (meta.side() != ConfigSide.CLIENT) {
            OELibConfig.LOGGER.warn("Registering non-client config {} into ClientConfigManager (side={})", meta.id(), meta.side());
        }
        var existing = CONFIGS.put(unit.id(), unit);
        if (existing != null) {
            OELibConfig.LOGGER.warn("Duplicate client config registration for {}, replacing previous unit", unit.id());
        }
        unit.applyAutoMigrationOnRegister();
    }

    static Optional<ConfigUnit<?>> get(ResourceLocation id) {
        return Optional.ofNullable(CONFIGS.get(id));
    }

    /**
     * Returns an unmodifiable snapshot of all registered client configurations.
     *
     * @return a map of configuration id to units
     */
    public static Map<ResourceLocation, ConfigUnit<?>> all() {
        return Map.copyOf(CONFIGS);
    }

    static void reloadAll() {
        for (ConfigUnit<?> unit : CONFIGS.values()) {
            unit.reload();
        }
    }


    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        reloadAll();
        OELibConfig.LOGGER.info("Successfully reload {} client config(s).", CONFIGS.size());
    }
}
