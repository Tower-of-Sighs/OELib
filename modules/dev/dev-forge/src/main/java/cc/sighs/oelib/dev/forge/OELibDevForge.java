package cc.sighs.oelib.dev.forge;

import cc.sighs.oelib.dev.OELibDev;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(OELibDev.MOD_ID)
public class OELibDevForge {
    public OELibDevForge() {
        OELibDev.init();        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OELibDevForgeClient::onClientSetup);
    }
}