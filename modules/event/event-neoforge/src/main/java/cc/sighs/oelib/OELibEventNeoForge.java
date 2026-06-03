package cc.sighs.oelib;

import cc.sighs.oelib.event.neoforge.TickBridge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibEvent.MOD_ID)
public class OELibEventNeoForge {

    public OELibEventNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        TickBridge.initServer();
        OELibEvent.init();
    }
}
