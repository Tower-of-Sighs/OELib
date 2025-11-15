package com.mafuyu404.oelib.fabric;

import com.mafuyu404.oelib.api.net.NetworkManager;
import com.mafuyu404.oelib.fabric.data.net.DataSyncChunkPacket;
import com.mafuyu404.oelib.fabric.example.FluidRenderExampleClient;
import net.fabricmc.api.ClientModInitializer;

public final class OELibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkManager.registerClientPacket(DataSyncChunkPacket.class, DataSyncChunkPacket.STREAM_CODEC);
        FluidRenderExampleClient.init();
    }
}