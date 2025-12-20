package com.sighs.oelib.data.net;

import com.sighs.oelib.OELib;
import com.sighs.oelib.network.ChunkAssembler;
import com.sighs.oelib.network.api.INetworkContext;
import com.sighs.oelib.network.api.INetworkPacket;
import com.sighs.oelib.network.api.NetworkPacket;
import com.sighs.oelib.network.api.Side;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * 数据同步分片数据包。
 */
@NetworkPacket(side = Side.BOTH, priority = 0)
public record DataSyncChunkPacket(UUID sessionId, int chunkIndex, int totalChunks, String dataClassName,
                                  byte[] chunkData) implements INetworkPacket<DataSyncChunkPacket> {

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
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(sessionId);
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
        buf.writeUtf(dataClassName);
        buf.writeInt(chunkData.length);
        buf.writeBytes(chunkData);
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