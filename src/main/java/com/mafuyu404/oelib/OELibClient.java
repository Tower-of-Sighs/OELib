package com.mafuyu404.oelib;

import com.mafuyu404.oelib.network.NetworkHandler;
import net.fabricmc.api.ClientModInitializer;

public class OELibClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkHandler.registerClient();
    }
}
