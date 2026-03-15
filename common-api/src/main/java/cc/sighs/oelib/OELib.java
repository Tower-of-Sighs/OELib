package cc.sighs.oelib;

import cc.sighs.oelib.data.DataRegistry;
import cc.sighs.oelib.dev.DevConfig;
import cc.sighs.oelib.dev.ExampleInit;
import cc.sighs.oelib.event.EventAutoRegistration;
import cc.sighs.oelib.icon.DynamicIconRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELib {
    public static final String MODID = "oelib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void init() {
        EventAutoRegistration.registerBasePackage("cc.sighs.oelib.dev.example.event");
        EventAutoRegistration.registerAllListeners();
        DevConfig.register();
        DynamicIconRegistry.forMod(MODID)
                .addNameFabric("icon1.png", 1)
                .addNameFabric("icon2.png", 1)
                .addNameForge("icon3.png", 1)
                .addNameForge("icon4.png", 1)
                .register();
        DataRegistry.initialize();
        DataRegistry.initializeExpressionEngine();
        ExampleInit.init();
    }
}
