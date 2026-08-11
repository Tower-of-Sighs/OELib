package cc.sighs.oelib.registry.forge;

import cc.sighs.oelib.registry.OELibRegistry;
import net.minecraftforge.fml.common.Mod;

@Mod(OELibRegistry.MOD_ID)
public class OELibRegistryForge {
    public OELibRegistryForge() {
        OELibRegistry.init();
    }
}
