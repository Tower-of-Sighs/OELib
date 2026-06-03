package cc.sighs.oelib;

import net.fabricmc.api.ModInitializer;

public class OELibDataFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibData.init();
    }
}
