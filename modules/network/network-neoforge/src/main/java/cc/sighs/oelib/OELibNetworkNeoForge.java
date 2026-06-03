package cc.sighs.oelib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibNetwork.MOD_ID)
public class OELibNetworkNeoForge {

    public OELibNetworkNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibNetwork.init();
    }
}
