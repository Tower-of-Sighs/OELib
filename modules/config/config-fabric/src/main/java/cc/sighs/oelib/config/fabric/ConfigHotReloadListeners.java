package cc.sighs.oelib.config.fabric;

import cc.sighs.oelib.OELibConfig;
import cc.sighs.oelib.config.ServerConfigManager;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public class ConfigHotReloadListeners extends ServerConfigManager implements IdentifiableResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(OELibConfig.MOD_ID, "config_hot_reload_server");
    }
}
