package com.mafuyu404.oelib.forge.data.net;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.forge.network.NetworkManager;
import com.mafuyu404.oelib.util.CodecUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

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
            OELib.LOGGER.warn("Cannot send packet: player is null");
            return;
        }
        sendToTarget(PacketDistributor.PLAYER.with(() -> player));
    }

    /**
     * 发送到所有玩家。
     */
    public void sendToAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            OELib.LOGGER.warn("Cannot send packet to all players: server instance is null");
            return;
        }
        sendToTarget(PacketDistributor.ALL.noArg());
    }

    private void sendToTarget(PacketDistributor.PacketTarget target) {
        try {
            Optional<String> jsonOpt = CodecUtils.encodeToJson(dataClass, data);
            if (jsonOpt.isEmpty()) {
                OELib.LOGGER.error("Failed to encode {} data to JSON", dataClass.getSimpleName());
                return;
            }

            String jsonData = jsonOpt.get();
            byte[] dataBytes = jsonData.getBytes(StandardCharsets.UTF_8);

            OELib.LOGGER.info("Sending {} data: {} entries, {} bytes",
                    dataClass.getSimpleName(), data.size(), dataBytes.length);

            if (dataBytes.length <= MAX_CHUNK_SIZE) {
                sendSingleChunk(target, dataBytes);
            } else {
                sendChunked(target, dataBytes);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send {} sync packet: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }

    private void sendSingleChunk(PacketDistributor.PacketTarget target, byte[] dataBytes) {
        try {
            UUID sessionId = UUID.randomUUID();
            DataSyncChunkPacket chunk = new DataSyncChunkPacket(
                    sessionId, 0, 1, dataClass.getName(), dataBytes);

            // 使用网络包框架发送
            if (target == PacketDistributor.ALL.noArg()) {
                chunk.sendToAll();
            } else {
                // 对于单个玩家，需要从 target 中提取玩家
                // 这里暂时保持原有方式，因为 PacketDistributor.PacketTarget 不容易提取玩家
                NetworkManager.getChannel().send(target, chunk);
            }

            OELib.LOGGER.debug("Sent single chunk for {} session {}", dataClass.getSimpleName(), sessionId);
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send single chunk for {}: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }

    private void sendChunked(PacketDistributor.PacketTarget target, byte[] data) {
        try {
            UUID sessionId = UUID.randomUUID();
            int totalChunks = (int) Math.ceil((double) data.length / MAX_CHUNK_SIZE);

            OELib.LOGGER.info("Splitting {} data into {} chunks for session {}",
                    dataClass.getSimpleName(), totalChunks, sessionId);

            for (int i = 0; i < totalChunks; i++) {
                int start = i * MAX_CHUNK_SIZE;
                int end = Math.min(start + MAX_CHUNK_SIZE, data.length);
                int chunkSize = end - start;

                byte[] chunkData = new byte[chunkSize];
                System.arraycopy(data, start, chunkData, 0, chunkSize);

                DataSyncChunkPacket chunk = new DataSyncChunkPacket(
                        sessionId, i, totalChunks, dataClass.getName(), chunkData);

                // 使用网络包框架发送
                if (target == PacketDistributor.ALL.noArg()) {
                    chunk.sendToAll();
                } else {
                    NetworkManager.getChannel().send(target, chunk);
                }

                OELib.LOGGER.debug("Sent chunk {}/{} ({} bytes) for {} session {}",
                        i + 1, totalChunks, chunkSize, dataClass.getSimpleName(), sessionId);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send chunked {} data: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }
}