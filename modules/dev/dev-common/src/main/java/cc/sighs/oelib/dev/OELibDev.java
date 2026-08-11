package cc.sighs.oelib.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELibDev {

    public static final String MOD_ID = "oelib_dev";
    public static final String MOD_NAME = "OELib Dev";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        DevConfig.register();
        ExampleInit.init();
    }
}
