package cc.sighs.oelib;

import net.fabricmc.api.ModInitializer;

public class OELibBlessFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibBless.init();
    }
}
