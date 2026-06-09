package cc.sighs.oelib.data.fabric;

import cc.sighs.oelib.data.OELibData;
import net.fabricmc.api.ModInitializer;

public class OELibDataFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibData.init();
    }
}
