package cc.sighs.oelib.data.forge;

import cc.sighs.oelib.data.OELibData;
import net.minecraftforge.fml.common.Mod;

@Mod(OELibData.MOD_ID)
public class OELibDataForge {
    public OELibDataForge() {
        OELibData.init();
    }
}
