package com.sighs.oelib.data.net;

import com.sighs.oelib.OELib;
import com.sighs.oelib.network.ChunkAssembler;
import com.sighs.oelib.network.api.INetworkContext;
import com.sighs.oelib.network.api.INetworkPacket;
import com.sighs.oelib.network.api.NetworkPacket;
import com.sighs.oelib.network.api.Side;

import java.util.UUID;

/**
 * Chunk of a data synchronization payload.
 *
 * @param sessionId     unique session identifier
 * @param chunkIndex    index of this chunk, starting from zero
 * @param totalChunks   total chunk count in this session
 * @param dataClassName fully qualified data class name
 * @param chunkData     raw JSON bytes of this chunk
 */
@NetworkPacket(modId = OELib.MODID, id = "data_sync_chunk", side = Side.BOTH, chunkThreshold = 30000)
public record DataSyncChunkPacket(UUID sessionId, int chunkIndex, int totalChunks, String dataClassName,
                                  byte[] chunkData) implements INetworkPacket<DataSyncChunkPacket> {

    @Override
    public void handle(INetworkContext context) {
        try {
            ChunkAssembler.receiveChunk(sessionId, chunkIndex, totalChunks, dataClassName, chunkData);
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to handle chunk packet: {}", e.getMessage(), e);
        }
    }
}