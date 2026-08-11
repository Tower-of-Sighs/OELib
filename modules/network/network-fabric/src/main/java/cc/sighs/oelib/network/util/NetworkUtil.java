package cc.sighs.oelib.network.util;

import cc.sighs.oelib.network.OELibNetwork;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.chunk.GenericChunkPacket;
import cc.sighs.oelib.platform.Platform;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public final class NetworkUtil {
    private NetworkUtil() {
    }

    public static boolean isLogicalServer() {
        return Platform.getCurrentServer() != null;
    }

    public static void sendChunkedPacket(byte[] data, ResourceLocation typeId, Iterable<ServerPlayer> players, int chunkSize) {
        try {
            forEachChunk(data, typeId, chunkSize, chunk -> {
                for (ServerPlayer player : players) {
                    chunk.sendTo(player);
                }
            });
        } catch (Exception e) {
            OELibNetwork.LOGGER.error("Failed to send chunked packet {}: {}", typeId, e.getMessage(), e);
        }
    }

    public static void sendChunkedPacketToAll(byte[] data, ResourceLocation typeId, int chunkSize) {
        try {
            forEachChunk(data, typeId, chunkSize, GenericChunkPacket::sendToAll);
        } catch (Exception e) {
            OELibNetwork.LOGGER.error("Failed broadcast chunked {}: {}", typeId, e.getMessage(), e);
        }
    }

    public static void sendChunkedPacketToServer(byte[] data, ResourceLocation typeId, int chunkSize) {
        try {
            forEachChunk(data, typeId, chunkSize, GenericChunkPacket::sendToServer);
        } catch (Exception e) {
            OELibNetwork.LOGGER.error("Failed client chunked {}: {}", typeId, e.getMessage(), e);
        }
    }

    public static <T extends INetworkPacket<T>> byte[] encodePacket(
            T packet,
            PacketInfo<T> packetInfo,
            RegistryFriendlyByteBuf buf) {
        packetInfo.codec().encode(buf, packet);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    public static <T extends INetworkPacket<T>> void sendWithChunking(
            T packet,
            PacketInfo<T> packetInfo,
            RegistryFriendlyByteBuf buf,
            int threshold,
            Runnable directSendAction,
            Consumer<byte[]> chunkedSendAction) {

        try {
            byte[] data = encodePacket(packet, packetInfo, buf);

            if (data.length <= threshold) {
                directSendAction.run();
            } else {
                chunkedSendAction.accept(data);
            }
        } finally {
            buf.release();
        }
    }

    public static RegistryFriendlyByteBuf createClientBuffer() {
        var client = Minecraft.getInstance();
        if (client.level == null) {
            throw new IllegalStateException("Cannot create buffer: client level is null");
        }
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), client.level.registryAccess());
    }

    public static RegistryFriendlyByteBuf createServerBuffer(ServerPlayer player) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
    }

    public static RegistryFriendlyByteBuf createBufferFromFirstPlayer(Collection<ServerPlayer> players) {
        if (players.isEmpty()) {
            throw new IllegalStateException("Cannot create buffer: player collection is empty");
        }
        var firstPlayer = players.iterator().next();
        return createServerBuffer(firstPlayer);
    }

    public static void forEachChunk(byte[] data, ResourceLocation typeId, int chunkSize, Consumer<GenericChunkPacket> consumer) {
        int totalChunks = validateChunkPlan(data, typeId, chunkSize);
        var sessionId = UUID.randomUUID();
        OELibNetwork.LOGGER.info("Chunking {} into {} chunks for session {} ({} bytes)", typeId, totalChunks, sessionId, data.length);
        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, data.length);
            byte[] chunkData = Arrays.copyOfRange(data, start, end);
            GenericChunkPacket chunk = new GenericChunkPacket(sessionId, data.length, (short) i, (short) totalChunks, typeId, chunkData);
            consumer.accept(chunk);
        }
    }

    private static int validateChunkPlan(byte[] data, ResourceLocation typeId, int chunkSize) {
        if (chunkSize < NetworkPacket.MIN_CHUNK_SIZE || chunkSize > NetworkPacket.MAX_CLIENTBOUND_CHUNK_SIZE) {
            throw new IllegalArgumentException("Invalid chunk size " + chunkSize + " for packet " + typeId
                    + "; expected " + NetworkPacket.MIN_CHUNK_SIZE + ".." + NetworkPacket.MAX_CLIENTBOUND_CHUNK_SIZE + " bytes");
        }
        if (data.length <= 0 || data.length > NetworkPacket.MAX_CHUNKED_PACKET_SIZE) {
            throw new IllegalArgumentException("Invalid chunked payload size " + data.length + " for packet " + typeId
                    + "; maximum is " + NetworkPacket.MAX_CHUNKED_PACKET_SIZE + " bytes");
        }

        long totalChunks = (data.length + (long) chunkSize - 1L) / chunkSize;
        if (totalChunks > NetworkPacket.MAX_CHUNK_COUNT) {
            throw new IllegalArgumentException("Refusing to split packet " + typeId + " into " + totalChunks
                    + " chunks; maximum is " + NetworkPacket.MAX_CHUNK_COUNT);
        }
        return (int) totalChunks;
    }

    public record PacketInfo<T extends INetworkPacket<T> & CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {}
}
