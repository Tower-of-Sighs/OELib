package com.mafuyu404.oelib.network;

import com.mafuyu404.oelib.OElib;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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
public record DataSyncChunkPacket(UUID sessionId, int chunkIndex, int totalChunks, String dataClassName,
                                  byte[] chunkData) implements CustomPacketPayload {

    public static final Type<DataSyncChunkPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OElib.MODID, "data_sync_chunk"));

    public static final StreamCodec<FriendlyByteBuf, DataSyncChunkPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(UUIDUtil.CODEC), DataSyncChunkPacket::sessionId,
            ByteBufCodecs.VAR_INT, DataSyncChunkPacket::chunkIndex,
            ByteBufCodecs.VAR_INT, DataSyncChunkPacket::totalChunks,
            ByteBufCodecs.STRING_UTF8, DataSyncChunkPacket::dataClassName,
            ByteBufCodecs.BYTE_ARRAY, DataSyncChunkPacket::chunkData,
            DataSyncChunkPacket::new
    );

    public static void handle(DataSyncChunkPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                ChunkAssembler.receiveChunk(packet.sessionId, packet.chunkIndex,
                        packet.totalChunks, packet.dataClassName, packet.chunkData);
            } catch (Exception e) {
                OElib.LOGGER.error("Failed to handle chunk packet: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}