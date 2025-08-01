package com.mafuyu404.oelib;

import com.mafuyu404.oelib.core.DataRegistry;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OElib implements ModInitializer {

    public static final String MODID = "oelib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        DataRegistry.initialize();
    }
}