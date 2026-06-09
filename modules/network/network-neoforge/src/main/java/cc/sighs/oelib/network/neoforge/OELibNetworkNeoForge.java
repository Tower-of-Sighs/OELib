package cc.sighs.oelib.network.neoforge;

import cc.sighs.oelib.network.OELibNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(cc.sighs.oelib.network.OELibNetwork.MOD_ID)
public class OELibNetworkNeoForge {

    public OELibNetworkNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibNetwork.init();
    }
}
