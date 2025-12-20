package com.sighs.oelib.forge;

import com.sighs.oelib.OELibClient;
import com.sighs.oelib.forge.example.FluidRenderExampleClient;

public final class OELibForgeClient {

    public static void onClientSetup() {
        OELibClient.initClient();
        FluidRenderExampleClient.onRegisterKeys();
    }
}
