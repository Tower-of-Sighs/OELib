package cc.sighs.oelib;

import cc.sighs.oelib.config.fabric.ConfigHotReloadListeners;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public class OELibConfigFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibConfig.init();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ConfigHotReloadListeners());
    }
}
