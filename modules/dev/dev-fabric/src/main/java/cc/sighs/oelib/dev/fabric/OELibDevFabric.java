package cc.sighs.oelib.dev.fabric;

import cc.sighs.oelib.dev.OELibDev;
import net.fabricmc.api.ModInitializer;

public class OELibDevFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibDev.init();
    }
}
