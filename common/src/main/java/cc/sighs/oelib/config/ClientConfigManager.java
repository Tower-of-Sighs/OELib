package cc.sighs.oelib.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.model.ConfigSide;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all client-side configurations and integrates with resource reload.
 * <p>
 * This manager is responsible for:
 * <ul>
 *     <li>Registering client configs backed by {@link ConfigUnit}</li>
 *     <li>Reloading client configs on resource reload</li>
 * </ul>
 * The public facade {@link ConfigManager} delegates client-related operations here.
 * </p>
 */
public class ClientConfigManager implements ResourceManagerReloadListener {
    private static final Map<Identifier, ConfigUnit<?>> CONFIGS = new ConcurrentHashMap<>();

    static void registerUnit(ConfigUnit<?> unit) {
        var meta = unit.meta();
        if (meta.side() != ConfigSide.CLIENT) {
            OELib.LOGGER.warn("Registering non-client config {} into ClientConfigManager (side={})", meta.id(), meta.side());
        }
        var existing = CONFIGS.put(unit.id(), unit);
        if (existing != null) {
            OELib.LOGGER.warn("Duplicate client config registration for {}, replacing previous unit", unit.id());
        }
        unit.applyAutoMigrationOnRegister();
    }

    static Optional<ConfigUnit<?>> get(Identifier id) {
        return Optional.ofNullable(CONFIGS.get(id));
    }

    public static Map<Identifier, ConfigUnit<?>> all() {
        return Map.copyOf(CONFIGS);
    }

    static void reloadAll() {
        for (ConfigUnit<?> unit : CONFIGS.values()) {
            unit.reload();
        }
    }


    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        reloadAll();
        OELib.LOGGER.info("Successfully reload {} client config(s).", CONFIGS.size());
    }
}
