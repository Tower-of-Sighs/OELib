package com.mafuyu404.oelib.network;

import com.mafuyu404.oelib.OElib;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class NetworkHandler {

    public static final ResourceLocation DATA_SYNC_CHUNK_PACKET = new ResourceLocation(OElib.MODID, "data_sync_chunk");

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                DATA_SYNC_CHUNK_PACKET,
                (client, handler, buf, responseSender) -> {
                    DataSyncChunkPacket packet = DataSyncChunkPacket.decode(buf);
                    client.execute(() -> DataSyncChunkPacket.handle(packet));
                });
        OElib.LOGGER.info("Registered network packets for data synchronization");
    }


    public static void sendTo(ServerPlayer player, DataSyncChunkPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.DATA_SYNC_CHUNK_PACKET, buf);
    }
}