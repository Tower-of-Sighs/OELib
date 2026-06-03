package cc.sighs.oelib;

import cc.sighs.oelib.misc.icon.DynamicIconRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELibMisc {

    public static final String MOD_ID = "oelib_misc";
    public static final String MOD_NAME = "OELib Misc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        DynamicIconRegistry.forMod("oelib")
                .addNameFabric("icon1.png", 1)
                .addNameFabric("icon2.png", 1)
                .addNameNeoForge("icon3.png", 1)
                .addNameNeoForge("icon4.png", 1)
                .register();
    }
}
