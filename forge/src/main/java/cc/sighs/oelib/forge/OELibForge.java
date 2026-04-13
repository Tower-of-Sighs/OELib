package cc.sighs.oelib.forge;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.forge.event.TickBridge;
import cc.sighs.oelib.forge.network.NetworkManagerImpl;
import cc.sighs.oelib.network.api.NetworkAutoRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(OELib.MODID)
public class OELibForge {
    public OELibForge() {
        NetworkManagerImpl.installAutoRegistrationHook();
        // Triggers the initial scan; discovered packets are registered via the hook.
        NetworkAutoRegistration.findAllAnnotatedPackets();
        OELib.init();
        TickBridge.initServer();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OELibForgeClient::onClientSetup);
    }
}
