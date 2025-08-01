package com.mafuyu404.oelib.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class NetworkHandler {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(DataSyncChunkPacket.TYPE, DataSyncChunkPacket.STREAM_CODEC);
    }

    public static void sendTo(ServerPlayer player, DataSyncChunkPacket packet) {
        ServerPlayNetworking.send(player, packet);
    }

}