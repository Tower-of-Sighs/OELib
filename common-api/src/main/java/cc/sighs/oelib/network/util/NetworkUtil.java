package cc.sighs.oelib.network.util;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.data.net.DataSyncChunkPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.UUID;

public final class NetworkUtil {
    private NetworkUtil() {
    }

    public static void sendChunkedPacket(byte[] data, String packetClassName, Iterable<ServerPlayer> players, int chunkSize) {
        try {
            var sessionId = UUID.randomUUID();
            int totalChunks = (int) Math.ceil((double) data.length / chunkSize);
            OELib.LOGGER.info("Splitting {} packet into {} chunks for session {} ({} bytes total)",
                    packetClassName, totalChunks, sessionId, data.length);

            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, data.length);
                byte[] chunkData = Arrays.copyOfRange(data, start, end);
                DataSyncChunkPacket chunk = new DataSyncChunkPacket(sessionId, i, totalChunks, packetClassName, chunkData);
                for (ServerPlayer player : players) {
                    chunk.sendTo(player);
                }
                OELib.LOGGER.debug("Sent chunk {}/{} ({} bytes) for {} session {}",
                        i + 1, totalChunks, chunkData.length, packetClassName, sessionId);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send chunked packet {}: {}", packetClassName, e.getMessage(), e);
        }
    }
}