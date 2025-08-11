package com.mafuyu404.oelib.forge.data.net;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.api.net.INetworkContext;
import com.mafuyu404.oelib.api.net.INetworkPacket;
import com.mafuyu404.oelib.api.net.NetworkPacket;
import com.mafuyu404.oelib.forge.network.ChunkAssembler;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * 数据同步分片数据包。
 *
 * @param sessionId     会话ID
 * @param chunkIndex    当前分片索引
 * @param totalChunks   总分片数
 * @param dataClassName 数据类名
 * @param chunkData     分片数据
 */
@NetworkPacket(priority = 0)
public record DataSyncChunkPacket(UUID sessionId, int chunkIndex, int totalChunks, String dataClassName,
                                  byte[] chunkData) implements INetworkPacket<DataSyncChunkPacket> {

    @Override
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

    @Override
    public void handle(INetworkContext context) {
        try {
            ChunkAssembler.receiveChunk(sessionId, chunkIndex,
                    totalChunks, dataClassName, chunkData);
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to handle chunk packet: {}", e.getMessage(), e);
        }
    }
}