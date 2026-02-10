package cc.sighs.oelib.fabric.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ServerConfigManager;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public class ConfigHotReloadListeners extends ServerConfigManager implements IdentifiableResourceReloadListener {
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(OELib.MODID, "config_hot_reload_server");
    }
}
