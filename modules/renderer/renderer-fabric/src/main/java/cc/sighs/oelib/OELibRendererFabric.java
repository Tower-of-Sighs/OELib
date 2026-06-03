package cc.sighs.oelib;

import net.fabricmc.api.ModInitializer;

public class OELibRendererFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibRenderer.init();
    }
}
