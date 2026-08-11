package cc.sighs.oelib.config.fabric;

import cc.sighs.oelib.config.OELibConfig;
import cc.sighs.oelib.config.ClientConfigManager;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public class ClientConfigHotReloadListeners extends ClientConfigManager implements IdentifiableResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(OELibConfig.MOD_ID, "config_hot_reload_client");
    }
}
