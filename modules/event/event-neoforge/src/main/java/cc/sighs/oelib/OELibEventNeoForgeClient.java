package cc.sighs.oelib;

import cc.sighs.oelib.event.neoforge.TickBridge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = OELibEvent.MOD_ID, dist = Dist.CLIENT)
public class OELibEventNeoForgeClient {
    public OELibEventNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        TickBridge.initClient();
    }
}
