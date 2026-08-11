package cc.sighs.oelib.config;

import cc.sighs.oelib.config.model.ConfigSide;
import com.flechazo.hkt.Maybe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers and reloads client-side configuration units.
 *
 * <p>A resource-manager reload reloads every registered unit. Duplicate identifiers replace the
 * previously registered unit.
 */
@ApiStatus.Internal
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

    static Maybe<ConfigUnit<?>> get(ResourceLocation id) {
        return Maybe.ofNullable(CONFIGS.get(id));
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
            ConfigLifecycle.reload(unit);
        }
    }


    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        reloadAll();
        OELibConfig.LOGGER.info("Successfully reload {} client config(s).", CONFIGS.size());
    }
}
