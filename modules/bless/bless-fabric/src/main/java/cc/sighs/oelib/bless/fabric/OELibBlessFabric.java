package cc.sighs.oelib.bless.fabric;

import cc.sighs.oelib.bless.OELibBless;
import net.fabricmc.api.ModInitializer;

public class OELibBlessFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibBless.init();
    }
}
