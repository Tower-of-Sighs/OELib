package cc.sighs.oelib.config.forge;

import cc.sighs.oelib.config.OELibConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(OELibConfig.MOD_ID)
public class OELibConfigForge {
    public OELibConfigForge() {
        OELibConfig.init();        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OELibConfigForgeClient::onClientSetup);
    }
}