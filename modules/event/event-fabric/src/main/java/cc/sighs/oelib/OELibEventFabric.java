package cc.sighs.oelib;

import cc.sighs.oelib.event.fabric.TickBridge;
import net.fabricmc.api.ModInitializer;

public class OELibEventFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibEvent.init();
        TickBridge.registerServer();
    }
}
