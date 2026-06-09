package cc.sighs.oelib.network.fabric;

import net.fabricmc.api.ModInitializer;

public class OELibNetworkFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NetworkManagerImpl.initialize();
    }
}
