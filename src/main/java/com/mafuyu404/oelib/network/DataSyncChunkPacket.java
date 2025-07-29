package com.mafuyu404.oelib.network;

import com.mafuyu404.oelib.OElib;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * 数据同步分片数据包。
 */
public record DataSyncChunkPacket(UUID sessionId, int chunkIndex, int totalChunks, String dataClassName,
                                  byte[] chunkData) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(sessionId);
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
        buf.writeUtf(dataClassName);
        buf.writeInt(chunkData.length);
        buf.writeBytes(chunkData);
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

    public static void handle(DataSyncChunkPacket packet) {
            try {
                ChunkAssembler.receiveChunk(packet.sessionId, packet.chunkIndex,
                        packet.totalChunks, packet.dataClassName, packet.chunkData);
            } catch (Exception e) {
                OElib.LOGGER.error("Failed to handle chunk packet: {}", e.getMessage(), e);
            }
    }
}