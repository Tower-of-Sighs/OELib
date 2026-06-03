package cc.sighs.oelib;

import net.fabricmc.api.ModInitializer;

public class OELibRegistryFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibRegistry.init();
    }
}
