package cc.sighs.oelib.bless.forge;

import cc.sighs.oelib.bless.OELibBless;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(OELibBless.MOD_ID)
public class OELibBlessForge {
    public OELibBlessForge() {
        OELibBless.init();        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OELibBlessForgeClient::onClientSetup);
    }
}