package cc.sighs.oelib;

import cc.sighs.oelib.network.fabric.NetworkManagerImpl;
import net.fabricmc.api.ModInitializer;

public class OELibNetworkFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NetworkManagerImpl.initialize();
    }
}
