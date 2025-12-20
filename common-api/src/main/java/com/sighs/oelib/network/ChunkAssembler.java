package com.sighs.oelib.network;

import com.sighs.oelib.OELib;
import com.sighs.oelib.data.DataManager;
import com.sighs.oelib.util.CodecUtils;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 分片组装器。
 * <p>
 * 负责接收和组装分片数据包。
 * </p>
 */
public class ChunkAssembler {

    private static final Map<UUID, AssemblySession> assemblingSessions = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ChunkAssembler-Cleanup");
        t.setDaemon(true);
        return t;
    });

    static {
        cleanupExecutor.scheduleAtFixedRate(ChunkAssembler::cleanupExpiredSessions, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 接收分片数据。
     *
     * @param sessionId     会话ID
     * @param chunkIndex    分片索引
     * @param totalChunks   总分片数
     * @param dataClassName 数据类名
     * @param chunkData     分片数据
     */
    public static void receiveChunk(UUID sessionId, int chunkIndex, int totalChunks, String dataClassName, byte[] chunkData) {
        AssemblySession session = assemblingSessions.computeIfAbsent(sessionId,
                id -> new AssemblySession(totalChunks, dataClassName));

        if (session.addChunk(chunkIndex, chunkData)) {
            try {
                byte[] completeData = session.assembleData();
                String jsonData = new String(completeData, StandardCharsets.UTF_8);

                Class<?> dataClass = Class.forName(dataClassName);
                @SuppressWarnings("unchecked")
                Optional<Map<ResourceLocation, ?>> dataOpt = (Optional<Map<ResourceLocation, ?>>) (Object)
                        CodecUtils.decodeFromJson((Class<Object>) dataClass, jsonData);

                if (dataOpt.isPresent()) {
                    Map<ResourceLocation, ?> data = dataOpt.get();
                    DataManager.updateClientDataRaw(dataClass, data);
                    OELib.LOGGER.info("Successfully processed {} {} data entries",
                            data.size(), dataClass.getSimpleName());
                } else {
                    OELib.LOGGER.error("Failed to parse JSON data for {} session {}", dataClassName, sessionId);
                }
            } catch (Exception e) {
                OELib.LOGGER.error("Failed to assemble chunk data for {} session {}: {}",
                        dataClassName, sessionId, e.getMessage(), e);
            } finally {
                assemblingSessions.remove(sessionId);
            }
        }
    }


    private static void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        assemblingSessions.entrySet().removeIf(entry -> {
            boolean expired = currentTime - entry.getValue().getCreationTime() > 60000;
            if (expired) {
                OELib.LOGGER.debug("Cleaning up expired assembly session: {}", entry.getKey());
            }
            return expired;
        });
    }

    private static class AssemblySession {
        private final int totalChunks;
        private final String dataClassName;
        private final byte[][] chunks;
        private final boolean[] received;
        private final long creationTime;
        private int receivedCount = 0;

        public AssemblySession(int totalChunks, String dataClassName) {
            this.totalChunks = totalChunks;
            this.dataClassName = dataClassName;
            this.chunks = new byte[totalChunks][];
            this.received = new boolean[totalChunks];
            this.creationTime = System.currentTimeMillis();
        }

        public synchronized boolean addChunk(int chunkIndex, byte[] chunkData) {
            if (chunkIndex < 0 || chunkIndex >= totalChunks) {
                OELib.LOGGER.warn("Invalid chunk index: {} (total: {}) for {}", chunkIndex, totalChunks, dataClassName);
                return false;
            }

            if (!received[chunkIndex]) {
                chunks[chunkIndex] = chunkData;
                received[chunkIndex] = true;
                receivedCount++;
            }

            return receivedCount == totalChunks;
        }

        public byte[] assembleData() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (int i = 0; i < totalChunks; i++) {
                if (chunks[i] == null) {
                    throw new IOException("Missing chunk: " + i + " for " + dataClassName);
                }
                output.write(chunks[i]);
            }
            return output.toByteArray();
        }

        public long getCreationTime() {
            return creationTime;
        }
    }
}