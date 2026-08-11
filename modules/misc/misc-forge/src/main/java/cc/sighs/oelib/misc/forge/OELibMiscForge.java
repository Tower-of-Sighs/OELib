package cc.sighs.oelib.misc.forge;

import cc.sighs.oelib.misc.OELibMisc;
import net.minecraftforge.fml.common.Mod;

@Mod(OELibMisc.MOD_ID)
public class OELibMiscForge {
    public OELibMiscForge() {
        OELibMisc.init();
    }
}