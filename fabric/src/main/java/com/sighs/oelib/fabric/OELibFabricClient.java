package com.sighs.oelib.fabric;

import com.sighs.oelib.OELibClient;
import com.sighs.oelib.data.net.DataSyncChunkPacket;
import com.sighs.oelib.fabric.example.FluidRenderExampleClient;
import com.sighs.oelib.network.api.NetworkManager;
import net.fabricmc.api.ClientModInitializer;

public final class OELibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OELibClient.initClient();
        NetworkManager.registerClientPacket(DataSyncChunkPacket.class, DataSyncChunkPacket.STREAM_CODEC);
        FluidRenderExampleClient.init();
    }
}