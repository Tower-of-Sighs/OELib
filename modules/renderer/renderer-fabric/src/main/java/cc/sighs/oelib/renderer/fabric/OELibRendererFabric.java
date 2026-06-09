package cc.sighs.oelib.renderer.fabric;

import cc.sighs.oelib.renderer.OELibRenderer;
import net.fabricmc.api.ModInitializer;

public class OELibRendererFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibRenderer.init();
    }
}
