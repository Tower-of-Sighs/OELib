package cc.sighs.oelib.network.chunk;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

@NetworkPacket(modId = OELib.MODID, id = "generic_chunk", side = Side.BOTH)
public record GenericChunkPacket(UUID sessionId, int totalSize, short chunkIndex, short totalChunks,
                                 ResourceLocation originalTypeId, byte[] chunkData)
        implements INetworkPacket<GenericChunkPacket> {

    @Override
    public void handle(INetworkContext context) {
        GenericChunkAssembler.receiveChunk(sessionId, totalSize, chunkIndex, totalChunks, originalTypeId, chunkData, context);
    }
}
