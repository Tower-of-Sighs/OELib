package cc.sighs.oelib.event.forge;

import cc.sighs.oelib.event.forge.TickBridge;

public final class OELibEventForgeClient {

    public static void onClientSetup() {
        TickBridge.initClient();
    }
}
