package cc.sighs.oelib;

import net.fabricmc.api.ModInitializer;

public class OELibDevFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibDev.init();
    }
}
