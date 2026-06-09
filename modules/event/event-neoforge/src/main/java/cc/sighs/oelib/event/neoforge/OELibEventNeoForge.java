package cc.sighs.oelib.event.neoforge;

import cc.sighs.oelib.event.OELibEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(cc.sighs.oelib.event.OELibEvent.MOD_ID)
public class OELibEventNeoForge {

    public OELibEventNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        TickBridge.initServer();
        OELibEvent.init();
    }
}
