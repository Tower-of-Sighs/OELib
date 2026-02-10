package cc.sighs.oelib.fabric.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ClientConfigManager;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public class ClientConfigHotReloadListeners extends ClientConfigManager implements IdentifiableResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(OELib.MODID, "config_hot_reload_client");
    }
}
