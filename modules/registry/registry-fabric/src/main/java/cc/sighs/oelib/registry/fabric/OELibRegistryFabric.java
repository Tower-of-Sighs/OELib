package cc.sighs.oelib.registry.fabric;

import cc.sighs.oelib.registry.OELibRegistry;
import net.fabricmc.api.ModInitializer;

public class OELibRegistryFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibRegistry.init();
    }
}
