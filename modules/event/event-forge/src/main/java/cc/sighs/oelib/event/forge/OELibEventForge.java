package cc.sighs.oelib.event.forge;

import cc.sighs.oelib.event.OELibEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(OELibEvent.MOD_ID)
public class OELibEventForge {
    public OELibEventForge() {
        TickBridge.initServer();
        OELibEvent.init();        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OELibEventForgeClient::onClientSetup);
    }
}