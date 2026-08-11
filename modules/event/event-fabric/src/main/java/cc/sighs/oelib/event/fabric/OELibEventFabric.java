package cc.sighs.oelib.event.fabric;

import cc.sighs.oelib.event.OELibEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ModInitializer;

public class OELibEventFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibEvent.init();
        TickBridge.registerServer();    }
}