package cc.sighs.oelib.renderer.forge;

import cc.sighs.oelib.renderer.OELibRenderer;
import net.minecraftforge.fml.common.Mod;

@Mod(OELibRenderer.MOD_ID)
public class OELibRendererForge {
    public OELibRendererForge() {
        OELibRenderer.init();
    }
}
