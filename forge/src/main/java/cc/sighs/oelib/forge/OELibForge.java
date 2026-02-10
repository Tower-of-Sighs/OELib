package cc.sighs.oelib.forge;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.forge.network.NetworkManagerImpl;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkAutoRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
@Mod(OELib.MODID)
public class OELibForge {
    public OELibForge() {
        NetworkManagerImpl impl = new NetworkManagerImpl();
        for (Class<? extends INetworkPacket<?>> packetClass : NetworkAutoRegistration.findAllAnnotatedPackets()) {
            impl.registerAnnotated(packetClass);
        }
        OELib.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OELibForgeClient::onClientSetup);
    }
}
