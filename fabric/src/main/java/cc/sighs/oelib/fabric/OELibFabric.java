package cc.sighs.oelib.fabric;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.fabric.config.ConfigHotReloadListeners;
import cc.sighs.oelib.fabric.network.NetworkManagerImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public class OELibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OELib.init();
        NetworkManagerImpl.initialize();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ConfigHotReloadListeners());
    }
}
