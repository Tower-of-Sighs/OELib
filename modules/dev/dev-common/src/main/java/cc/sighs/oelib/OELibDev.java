package cc.sighs.oelib;

import cc.sighs.oelib.dev.DevConfig;
import cc.sighs.oelib.dev.ExampleInit;
import cc.sighs.oelib.event.EventAutoRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELibDev {

    public static final String MOD_ID = "oelib_dev";
    public static final String MOD_NAME = "OELib Dev";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        EventAutoRegistration.registerBasePackage("cc.sighs.oelib.dev.example.event");
        DevConfig.register();
        ExampleInit.init();
    }
}
