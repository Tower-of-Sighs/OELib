package cc.sighs.oelib.data.net;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import cc.sighs.oelib.network.chunk.GenericChunkAssembler;

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
            GenericChunkAssembler.receiveChunkJson(sessionId, chunkIndex, totalChunks, dataClassName, chunkData);
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to handle data sync chunk: {}", e.getMessage(), e);
        }
    }
}