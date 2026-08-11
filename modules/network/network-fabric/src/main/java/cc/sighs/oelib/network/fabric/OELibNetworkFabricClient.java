package cc.sighs.oelib.network.fabric;

import net.fabricmc.api.ClientModInitializer;

public class OELibNetworkFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkManagerImpl.initializeClient();    }
}