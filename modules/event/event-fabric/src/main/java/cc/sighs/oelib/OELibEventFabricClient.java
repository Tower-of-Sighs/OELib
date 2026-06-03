package cc.sighs.oelib;

import cc.sighs.oelib.event.fabric.TickBridge;
import net.fabricmc.api.ClientModInitializer;

public class OELibEventFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TickBridge.registerClient();
    }
}
