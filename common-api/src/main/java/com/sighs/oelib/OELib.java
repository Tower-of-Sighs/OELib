package com.sighs.oelib;

import com.sighs.oelib.data.DataRegistry;
import com.sighs.oelib.example.ExampleCreativeTab;
import com.sighs.oelib.example.ExampleRegistry;
import com.sighs.oelib.icon.DynamicIconRegistry;
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
        DynamicIconRegistry.forMod(MODID)
                .addNameFabric("icon1.png", 1)
                .addNameFabric("icon2.png", 1)
                .addNameForge("icon3.png", 1)
                .addNameForge("icon4.png", 1)
                .register();
        DataRegistry.initialize();
        DataRegistry.initializeExpressionEngine();
    }
}
