package com.mafuyu404.oelib.network;

import com.mafuyu404.oelib.OElib;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 数据同步分片数据包。
 *
 * @param sessionId     会话ID
 * @param chunkIndex    当前分片索引
 * @param totalChunks   总分片数
 * @param dataClassName 数据类名
 * @param chunkData     分片数据
 */
public record DataSyncChunkPacket(UUID sessionId, int chunkIndex, int totalChunks, String dataClassName,
                                  byte[] chunkData) {

    public static void encode(DataSyncChunkPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.sessionId);
        buf.writeInt(packet.chunkIndex);
        buf.writeInt(packet.totalChunks);
        buf.writeUtf(packet.dataClassName);
        buf.writeInt(packet.chunkData.length);
        buf.writeBytes(packet.chunkData);
    }

    public static DataSyncChunkPacket decode(FriendlyByteBuf buf) {
        UUID sessionId = buf.readUUID();
        int chunkIndex = buf.readInt();
        int totalChunks = buf.readInt();
        String dataClassName = buf.readUtf();
        int dataLength = buf.readInt();
        byte[] chunkData = new byte[dataLength];
        buf.readBytes(chunkData);

        return new DataSyncChunkPacket(sessionId, chunkIndex, totalChunks, dataClassName, chunkData);
    }

    public static void handle(DataSyncChunkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                ChunkAssembler.receiveChunk(packet.sessionId, packet.chunkIndex,
                        packet.totalChunks, packet.dataClassName, packet.chunkData);
            } catch (Exception e) {
                OElib.LOGGER.error("Failed to handle chunk packet: {}", e.getMessage(), e);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}