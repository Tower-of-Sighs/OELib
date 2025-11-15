package com.mafuyu404.oelib.fabric;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.fabric.network.NetworkManager;
import net.fabricmc.api.ModInitializer;

public class OELibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OELib.init();
        NetworkManager.initialize();
    }
}
