package cc.sighs.oelib.network.forge;

import cc.sighs.oelib.network.OELibNetwork;
import net.minecraftforge.fml.common.Mod;

@Mod(OELibNetwork.MOD_ID)
public class OELibNetworkForge {
    public OELibNetworkForge() {
        NetworkManagerImpl.registerAll();
        OELibNetwork.init();
    }
}
