package com.mafuyu404.oelib.network;

import com.mafuyu404.oelib.OElib;
import com.mafuyu404.oelib.util.CodecUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

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

    public void sendTo(ServerPlayer player) {
        if (player == null) {
            OElib.LOGGER.warn("Cannot send packet: player is null");
            return;
        }
        encodeAndSend(dataBytes -> sendToTarget(dataBytes, player));
    }

    public void sendToAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            OElib.LOGGER.warn("Cannot send packet to all players: server instance is null");
            return;
        }
        encodeAndSend(this::sendToAllPlayers);
    }

    private void encodeAndSend(Consumer<byte[]> sender) {
        try {
            Optional<String> jsonOpt = CodecUtils.encodeToJson(dataClass, data);
            if (jsonOpt.isEmpty()) {
                OElib.LOGGER.error("Failed to encode {} data to JSON", dataClass.getSimpleName());
                return;
            }

            byte[] dataBytes = jsonOpt.get().getBytes(StandardCharsets.UTF_8);
            OElib.LOGGER.info("Sending {} data: {} entries, {} bytes",
                    dataClass.getSimpleName(), data.size(), dataBytes.length);

            sender.accept(dataBytes);
        } catch (Exception e) {
            OElib.LOGGER.error("Failed to send {} sync packet: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }

    private void sendToTarget(byte[] dataBytes, ServerPlayer player) {
        if (dataBytes.length <= MAX_CHUNK_SIZE) {
            sendChunk(new DataSyncChunkPacket(UUID.randomUUID(), 0, 1, dataClass.getName(), dataBytes),
                    chunk -> PacketDistributor.sendToPlayer(player, chunk));
        } else {
            sendChunked(dataBytes, chunk -> PacketDistributor.sendToPlayer(player, chunk));
        }
    }

    private void sendToAllPlayers(byte[] dataBytes) {
        if (dataBytes.length <= MAX_CHUNK_SIZE) {
            sendChunk(new DataSyncChunkPacket(UUID.randomUUID(), 0, 1, dataClass.getName(), dataBytes),
                    PacketDistributor::sendToAllPlayers);
        } else {
            sendChunked(dataBytes, PacketDistributor::sendToAllPlayers);
        }
    }

    private void sendChunked(byte[] data, Consumer<DataSyncChunkPacket> sendFunc) {
        UUID sessionId = UUID.randomUUID();
        int totalChunks = (int) Math.ceil((double) data.length / MAX_CHUNK_SIZE);

        OElib.LOGGER.info("Splitting {} data into {} chunks for session {}",
                dataClass.getSimpleName(), totalChunks, sessionId);

        for (int i = 0; i < totalChunks; i++) {
            int start = i * MAX_CHUNK_SIZE;
            int end = Math.min(start + MAX_CHUNK_SIZE, data.length);

            byte[] chunkData = Arrays.copyOfRange(data, start, end);
            DataSyncChunkPacket chunk = new DataSyncChunkPacket(sessionId, i, totalChunks, dataClass.getName(), chunkData);

            sendChunk(chunk, sendFunc);
        }
    }

    private void sendChunk(DataSyncChunkPacket chunk, Consumer<DataSyncChunkPacket> sendFunc) {
        try {
            sendFunc.accept(chunk);
            OElib.LOGGER.debug("Sent chunk {}/{} ({} bytes) for {} session {}",
                    chunk.chunkIndex() + 1, chunk.totalChunks(), chunk.chunkData().length,
                    dataClass.getSimpleName(), chunk.sessionId());
        } catch (Exception e) {
            OElib.LOGGER.error("Failed to send chunk for {}: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }
}
