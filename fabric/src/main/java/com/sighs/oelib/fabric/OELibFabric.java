package com.sighs.oelib.fabric;

import com.sighs.oelib.OELib;
import com.sighs.oelib.fabric.network.NetworkManagerImpl;
import net.fabricmc.api.ModInitializer;

public class OELibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OELib.init();
        NetworkManagerImpl.initialize();
    }
}
