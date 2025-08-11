package com.mafuyu404.oelib.fabric;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.fabric.data.DataRegistry;
import com.mafuyu404.oelib.fabric.network.NetworkManager;
import net.fabricmc.api.ModInitializer;

public final class OELibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        OELib.init();
        DataRegistry.initialize();
        NetworkManager.initialize();
    }
}
