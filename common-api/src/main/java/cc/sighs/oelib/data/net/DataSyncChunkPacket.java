package cc.sighs.oelib.data.net;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.network.ChunkAssembler;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
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
@NetworkPacket(side = Side.BOTH, chunkThreshold = 30000)
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