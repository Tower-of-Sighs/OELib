package cc.sighs.oelib;

import cc.sighs.oelib.data.DataRegistry;
import cc.sighs.oelib.example.ExampleCreativeTab;
import cc.sighs.oelib.example.ExampleMenus;
import cc.sighs.oelib.example.ExampleRegistry;
import cc.sighs.oelib.icon.DynamicIconRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELib {
    public static final String MODID = "oelib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void init() {
        ExampleRegistry.init();
        ExampleCreativeTab.init();
        ExampleCreativeTab.registerCreativeTabEntries();
        ExampleCreativeTab.modifyCreativeTab();
        ExampleRegistry.registerFuel();
        ExampleMenus.init();
        DynamicIconRegistry.forMod(MODID)
                .addNameFabric("icon1.png", 1)
                .addNameFabric("icon2.png", 1)
                .addNameNeoForge("icon3.png", 1)
                .addNameNeoForge("icon4.png", 1)
                .register();
        DataRegistry.initialize();
        DataRegistry.initializeExpressionEngine();
    }
}
