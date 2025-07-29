package com.mafuyu404.oelib.network;

import com.mafuyu404.oelib.OElib;
import com.mafuyu404.oelib.util.CodecUtils;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 数据同步数据包。
 * <p>
 * 负责将服务器端的数据同步到客户端，支持大数据分块传输。
 * </p>
 *
 * @param <T> 数据类型
 */
public class DataSyncPacket<T> {

    private static final int MAX_CHUNK_SIZE = 30000; // 30KB
    private static MinecraftServer currentServer = null;

    private final Class<T> dataClass;
    private final Map<ResourceLocation, T> data;

    public DataSyncPacket(Class<T> dataClass, Map<ResourceLocation, T> data) {
        this.dataClass = dataClass;
        this.data = data;
    }


    /**
     * 发送到指定玩家
     */
    public void sendTo(ServerPlayer player) {
        if (player == null) {
            OElib.LOGGER.warn("Cannot send packet: player is null");
            return;
        }
        sendToTarget(player);
    }

    /**
     * 发送到所有玩家。
     */
    public void sendToAll() {
        MinecraftServer server = getCurrentServer();
        if (server == null) {
            OElib.LOGGER.warn("Cannot send packet to all players: server instance is null");
            return;
        }

        for (ServerPlayer player : PlayerLookup.all(server)) {
            sendToTarget(player);
        }
    }

    private void sendToTarget(ServerPlayer player) {
        try {
            Optional<String> jsonOpt = CodecUtils.encodeToJson(dataClass, data);
            if (jsonOpt.isEmpty()) {
                OElib.LOGGER.error("Failed to encode {} data to JSON", dataClass.getSimpleName());
                return;
            }

            String jsonData = jsonOpt.get();
            byte[] dataBytes = jsonData.getBytes(StandardCharsets.UTF_8);

            OElib.LOGGER.info("Sending {} data: {} entries, {} bytes",
                    dataClass.getSimpleName(), data.size(), dataBytes.length);

            if (dataBytes.length <= MAX_CHUNK_SIZE) {
                sendSingleChunk(player, dataBytes);
            } else {
                sendChunked(player, dataBytes);
            }
        } catch (Exception e) {
            OElib.LOGGER.error("Failed to send {} sync packet: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }

    private void sendSingleChunk(ServerPlayer player, byte[] dataBytes) {
        try {
            UUID sessionId = UUID.randomUUID();
            DataSyncChunkPacket chunk = new DataSyncChunkPacket(
                    sessionId, 0, 1, dataClass.getName(), dataBytes);
            NetworkHandler.sendTo(player, chunk);
            OElib.LOGGER.debug("Sent single chunk for {} session {}", dataClass.getSimpleName(), sessionId);
        } catch (Exception e) {
            OElib.LOGGER.error("Failed to send single chunk for {}: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }

    private void sendChunked(ServerPlayer player, byte[] data) {
        try {
            UUID sessionId = UUID.randomUUID();
            int totalChunks = (int) Math.ceil((double) data.length / MAX_CHUNK_SIZE);

            OElib.LOGGER.info("Splitting {} data into {} chunks for session {}",
                    dataClass.getSimpleName(), totalChunks, sessionId);

            for (int i = 0; i < totalChunks; i++) {
                int start = i * MAX_CHUNK_SIZE;
                int end = Math.min(start + MAX_CHUNK_SIZE, data.length);
                int chunkSize = end - start;

                byte[] chunkData = new byte[chunkSize];
                System.arraycopy(data, start, chunkData, 0, chunkSize);

                DataSyncChunkPacket chunk = new DataSyncChunkPacket(
                        sessionId, i, totalChunks, dataClass.getName(), chunkData);
                NetworkHandler.sendTo(player, chunk);

                OElib.LOGGER.debug("Sent chunk {}/{} ({} bytes) for {} session {}",
                        i + 1, totalChunks, chunkSize, dataClass.getSimpleName(), sessionId);
            }
        } catch (Exception e) {
            OElib.LOGGER.error("Failed to send chunked {} data: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }

    private MinecraftServer getCurrentServer() {
        return currentServer;
    }
}