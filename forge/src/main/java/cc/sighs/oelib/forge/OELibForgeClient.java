package cc.sighs.oelib.forge;

import cc.sighs.oelib.OELibClient;
import cc.sighs.oelib.forge.example.FluidRenderExampleClient;

public final class OELibForgeClient {

    public static void onClientSetup() {
        OELibClient.initClient();
        FluidRenderExampleClient.onRegisterKeys();
    }
}
