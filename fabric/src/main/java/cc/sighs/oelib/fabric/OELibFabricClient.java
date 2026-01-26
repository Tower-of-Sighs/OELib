package cc.sighs.oelib.fabric;

import cc.sighs.oelib.OELibClient;
import cc.sighs.oelib.fabric.example.FluidRenderExampleClient;
import net.fabricmc.api.ClientModInitializer;

public class OELibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OELibClient.initClient();
        FluidRenderExampleClient.init();
    }
}