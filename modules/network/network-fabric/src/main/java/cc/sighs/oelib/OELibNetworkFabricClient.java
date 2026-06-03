package cc.sighs.oelib;

import cc.sighs.oelib.network.fabric.NetworkManagerImpl;
import net.fabricmc.api.ClientModInitializer;

public class OELibNetworkFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkManagerImpl.initializeClient();
    }
}
