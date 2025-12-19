package com.sighs.oelib.neoforge.data.net;

import com.sighs.oelib.OELib;
import com.sighs.oelib.neoforge.network.ChunkAssembler;
import com.sighs.oelib.network.net.INetworkContext;
import com.sighs.oelib.network.net.NetworkPacket;
import com.sighs.oelib.network.net.Side;
import com.sighs.oelib.network.net.SimplePacket;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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
                                  byte[] chunkData) implements SimplePacket<DataSyncChunkPacket> {
    public static final StreamCodec<FriendlyByteBuf, DataSyncChunkPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(UUIDUtil.CODEC), DataSyncChunkPacket::sessionId,
            ByteBufCodecs.VAR_INT, DataSyncChunkPacket::chunkIndex,
            ByteBufCodecs.VAR_INT, DataSyncChunkPacket::totalChunks,
            ByteBufCodecs.STRING_UTF8, DataSyncChunkPacket::dataClassName,
            ByteBufCodecs.BYTE_ARRAY, DataSyncChunkPacket::chunkData,
            DataSyncChunkPacket::new
    );

    @Override
    public String getModId() {
        return OELib.MODID;
    }

    @Override
    public void handle(INetworkContext context) {
        try {
            ChunkAssembler.receiveChunk(sessionId, chunkIndex, totalChunks, dataClassName, chunkData);
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to handle chunk packet: {}", e.getMessage(), e);
        }
    }
}