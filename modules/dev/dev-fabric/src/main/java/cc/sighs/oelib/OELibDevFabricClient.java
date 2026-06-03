package cc.sighs.oelib;

import net.fabricmc.api.ClientModInitializer;

public class OELibDevFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OELibDevClient.init();
    }
}
