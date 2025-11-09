package com.mafuyu404.oelib;

import com.mafuyu404.oelib.data.DataManagerBridge;
import com.mafuyu404.oelib.data.DataRegistry;
import com.mafuyu404.oelib.icon.DynamicIconRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OELib {
    public static final String MODID = "oelib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void init() {
        DynamicIconRegistry.forMod(MODID)
                .addNameFabric("icon1.png", 1)
                .addNameFabric("icon2.png", 1)
                .addNameForge("icon3.png", 1)
                .addNameForge("icon4.png", 1)
                .register();
        DataRegistry.initialize();
        DataManagerBridge.attachReloadListenersSorted(
                DataRegistry.getRegisteredTypesByPriority()
        );
        if (!Boolean.getBoolean("oelib.skipExpressionEngine")) {
            DataRegistry.initializeExpressionEngine();
        }
    }
}
