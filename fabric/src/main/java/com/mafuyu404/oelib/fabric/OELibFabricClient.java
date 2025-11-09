package com.mafuyu404.oelib.fabric;

import com.mafuyu404.oelib.fabric.data.net.DataSyncChunkPacket;
import com.mafuyu404.oelib.fabric.network.NetworkManager;
import com.mafuyu404.oelib.fabric.example.FluidRenderExampleClient;
import net.fabricmc.api.ClientModInitializer;

public class OELibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkManager.registerClientPacket(DataSyncChunkPacket.class);
        FluidRenderExampleClient.init();
    }
}