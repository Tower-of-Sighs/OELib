package cc.sighs.oelib.fabric;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.fabric.network.NetworkManager;
import net.fabricmc.api.ModInitializer;

public class OELibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OELib.init();
        NetworkManager.initialize();
    }
}
