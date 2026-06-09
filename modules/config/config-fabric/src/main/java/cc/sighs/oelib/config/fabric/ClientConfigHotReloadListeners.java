package cc.sighs.oelib.config.fabric;

import cc.sighs.oelib.config.ClientConfigManager;
import cc.sighs.oelib.config.OELibConfig;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public class ClientConfigHotReloadListeners extends ClientConfigManager implements IdentifiableResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(OELibConfig.MOD_ID, "config_hot_reload_client");
    }
}
