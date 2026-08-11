package cc.sighs.oelib.network.chunk;

import cc.sighs.oelib.network.OELibNetwork;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacketTypes;
import cc.sighs.oelib.network.serialization.NetworkSerialization;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.BitSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class GenericChunkAssembler {
    private static final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private static final AtomicLong BYTES_IN_ASSEMBLY = new AtomicLong(0);
    private static final long QUOTA_BYTES = 64L * 1024L * 1024L; // 64MB global quota
    private static final int MAX_SESSIONS = 1024;
    private static final long EXPIRE_MS = 30_000L;
    private static final ScheduledExecutorService CLEANUP = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GenericChunkAssembler-Cleanup");
        t.setDaemon(true);
        return t;
    });

    static {
        CLEANUP.scheduleAtFixedRate(() -> {
            try {
                sessions.entrySet().removeIf(e -> {
                    Session s = e.getValue();
                    if (s.expired()) {
                        OELibNetwork.LOGGER.debug("Cleanup expired chunk session {}", e.getKey());
                        s.releaseAll();
                        return true;
                    }
                    return false;
                });
            } catch (Throwable t) {
                OELibNetwork.LOGGER.warn("Cleanup task failed", t);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private GenericChunkAssembler() {
    }

    public static void receiveChunk(UUID sessionId, int totalSize, short chunkIndex, short totalChunks,
                                    ResourceLocation typeId, byte[] chunkData, INetworkContext context) {
        long maximumChunksForSize = (totalSize + (long) NetworkPacket.MIN_CHUNK_SIZE - 1L) / NetworkPacket.MIN_CHUNK_SIZE;
        if (totalSize <= 0 || totalSize > NetworkPacket.MAX_CHUNKED_PACKET_SIZE
                || totalChunks > NetworkPacket.MAX_CHUNK_COUNT
                || totalChunks > maximumChunksForSize
                || chunkIndex < 0 || chunkIndex >= totalChunks
                || chunkData.length <= 0 || chunkData.length > NetworkPacket.MAX_CLIENTBOUND_CHUNK_SIZE) {
            OELibNetwork.LOGGER.warn("Reject invalid chunk session {}: size={}, index={}/{}, chunkBytes={}, type={}",
                    sessionId, totalSize, chunkIndex, totalChunks, chunkData.length, typeId);
            return;
        }

        if (!sessions.containsKey(sessionId) && sessions.size() >= MAX_SESSIONS) {
            OELibNetwork.LOGGER.warn("Chunk assembly session limit exceeded, reject session {}", sessionId);
            return;
        }

        var s = sessions.computeIfAbsent(sessionId, id -> {
            // quota check
            long after = BYTES_IN_ASSEMBLY.addAndGet(totalSize);
            if (after > QUOTA_BYTES) {
                BYTES_IN_ASSEMBLY.addAndGet(-totalSize);
                OELibNetwork.LOGGER.warn("Chunk assembly quota exceeded: {} bytes in assembly, reject session {}", after, id);
                return null;
            }
            return new Session(totalChunks, totalSize, typeId);
        });
        if (s == null) {
            return;
        }

        if (!s.matches(totalChunks, totalSize, typeId)) {
            OELibNetwork.LOGGER.warn("Reject inconsistent metadata for chunk session {}", sessionId);
            if (sessions.remove(sessionId, s)) {
                s.releaseAll();
            }
            return;
        }

        AddResult result = s.add(chunkIndex, chunkData);
        if (result == AddResult.REJECTED) {
            if (sessions.remove(sessionId, s)) {
                s.releaseAll();
            }
            return;
        }
        if (result == AddResult.COMPLETE) {
            if (!sessions.remove(sessionId, s)) {
                return;
            }
            CompositeByteBuf composite = null;
            try {
                composite = s.assembleComposite();
                var clazz = NetworkPacketTypes.classOf(typeId);
                if (clazz == null || !CustomPacketPayload.class.isAssignableFrom(clazz)) {
                    OELibNetwork.LOGGER.warn("Chunk target {} is not a registered CustomPacketPayload", typeId);
                    return;
                }
                @SuppressWarnings("unchecked")
                Class<CustomPacketPayload> c = (Class<CustomPacketPayload>) clazz;
                var codec = NetworkSerialization.autoCodec(c);
                var buf = new RegistryFriendlyByteBuf(composite, context.registryAccess());
                var payload = codec.decode(buf);
                try {
                    if (payload instanceof INetworkPacket<?> p) {
                        context.enqueueWork(() -> p.handle(context));
                    } else {
                        OELibNetwork.LOGGER.warn("Decoded payload {} does not implement INetworkPacket", typeId);
                    }
                } catch (Throwable t) {
                    OELibNetwork.LOGGER.error("Failed to dispatch reassembled payload {}", typeId, t);
                }
            } catch (Throwable t) {
                OELibNetwork.LOGGER.error("Failed to reassemble payload {}", typeId, t);
            } finally {
                ReferenceCountUtil.release(composite);
            }
        }
    }

    private enum AddResult {
        INCOMPLETE,
        COMPLETE,
        REJECTED
    }

    private static final class Session {
        final int total;
        final int totalSize;
        final ResourceLocation typeId;
        final BitSet received;
        final ByteBuf[] parts;
        final long startMs;
        int count;
        int receivedBytes;
        boolean quotaReleased;

        Session(int totalChunks, int totalSize, ResourceLocation typeId) {
            this.total = totalChunks;
            this.totalSize = totalSize;
            this.typeId = typeId;
            this.received = new BitSet(totalChunks);
            this.parts = new ByteBuf[totalChunks];
            this.count = 0;
            this.receivedBytes = 0;
            this.startMs = System.currentTimeMillis();
        }

        boolean matches(int expectedTotal, int expectedSize, ResourceLocation expectedType) {
            return this.total == expectedTotal && this.totalSize == expectedSize && this.typeId.equals(expectedType);
        }

        synchronized AddResult add(int idx, byte[] data) {
            if (!received.get(idx)) {
                if ((long) receivedBytes + data.length > totalSize) {
                    OELibNetwork.LOGGER.warn("Chunk data exceeds declared size {} for {}", totalSize, typeId);
                    return AddResult.REJECTED;
                }
                parts[idx] = Unpooled.wrappedBuffer(data);
                received.set(idx);
                count++;
                receivedBytes += data.length;
            }
            if (count != total) {
                return AddResult.INCOMPLETE;
            }
            if (receivedBytes != totalSize) {
                OELibNetwork.LOGGER.warn("Chunk data size {} does not match declared size {} for {}", receivedBytes, totalSize, typeId);
                return AddResult.REJECTED;
            }
            return AddResult.COMPLETE;
        }

        synchronized CompositeByteBuf assembleComposite() {
            var composite = Unpooled.compositeBuffer(total);
            try {
                for (int i = 0; i < total; i++) {
                    if (parts[i] == null) {
                        throw new IllegalStateException("Missing chunk " + i + " for " + typeId);
                    }
                    ByteBuf part = parts[i];
                    parts[i] = null;
                    composite.addComponent(true, part);
                }
                releaseQuota();
                return composite;
            } catch (Throwable t) {
                ReferenceCountUtil.release(composite);
                releaseAll();
                throw t;
            }
        }

        boolean expired() {
            return System.currentTimeMillis() - startMs > EXPIRE_MS;
        }

        synchronized void releaseAll() {
            for (int i = 0; i < total; i++) {
                if (parts[i] != null) {
                    ReferenceCountUtil.release(parts[i]);
                    parts[i] = null;
                }
            }
            releaseQuota();
        }

        private synchronized void releaseQuota() {
            if (!quotaReleased) {
                quotaReleased = true;
                BYTES_IN_ASSEMBLY.addAndGet(-totalSize);
            }
        }
    }
}
