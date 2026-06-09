package cc.sighs.oelib.event.fabric;

import net.fabricmc.api.ClientModInitializer;

public class OELibEventFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TickBridge.registerClient();
    }
}
