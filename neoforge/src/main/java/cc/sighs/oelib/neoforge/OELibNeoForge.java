package cc.sighs.oelib.neoforge;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.neoforge.event.TickBridge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELib.MODID)
public final class OELibNeoForge {
    public OELibNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        TickBridge.initServer();
        OELib.init();
    }
}
