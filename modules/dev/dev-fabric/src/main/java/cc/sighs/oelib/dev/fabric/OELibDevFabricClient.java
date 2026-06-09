package cc.sighs.oelib.dev.fabric;

import cc.sighs.oelib.dev.OELibDevClient;
import net.fabricmc.api.ClientModInitializer;

public class OELibDevFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OELibDevClient.init();
    }
}
