package com.sighs.oelib.fabric.network;

import com.sighs.oelib.OELib;
import com.sighs.oelib.data.net.DataSyncChunkPacket;
import com.sighs.oelib.fabric.data.DataManager;
import com.sighs.oelib.network.api.*;
import com.sighs.oelib.network.serialization.NetworkSerialization;
import com.sighs.oelib.network.spi.INetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Fabric implementation of the shared networking API.
 * <p>
 * Bridges {@link com.sighs.oelib.network.api.INetworkPacket} to Fabric's
 * play stage networking facilities.
 * </p>
 */
public class NetworkManagerImpl implements INetworkManager {

    private static final Map<CustomPacketPayload.Type<?>, PacketInfo<?>> registeredPackets = new ConcurrentHashMap<>();

    public static void initialize() {
        registerBuiltinPackets();
        OELib.LOGGER.info("Network manager initialized");
    }

    public <T extends INetworkPacket<T> & CustomPacketPayload> void registerBidirectionalPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        registerServerOrBidirectionalPacket(packetClass, codec);
    }

    public <T extends INetworkPacket<T> & CustomPacketPayload> void registerClientPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        try {
            // 通过类名生成类型信息，避免创建临时实例
            CustomPacketPayload.Type<T> type = createPacketType(packetClass);

            ClientPlayNetworking.registerGlobalReceiver(type, (packet, context) -> context.client().execute(() -> {
                FabricNetworkContext networkContext = new FabricNetworkContext(null, false);
                packet.handle(networkContext);
            }));

            OELib.LOGGER.info("Registered client network packet: {} (ID: {})",
                    type.id().getPath(), type.id());

        } catch (Exception e) {
            throw new RuntimeException("Failed to register client packet " + packetClass.getSimpleName(), e);
        }
    }

    public <T extends INetworkPacket<T> & CustomPacketPayload> void registerServerPacket(Class<T> packetClass, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        registerServerOrBidirectionalPacket(packetClass, codec);
    }

    /**
     * Returns the number of registered packet types.
     *
     * @return registered packet type count
     */
    public static int getRegisteredPacketCount() {
        return registeredPackets.size();
    }

    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerServerOrBidirectionalPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        try {
            CustomPacketPayload.Type<T> type = createPacketType(packetClass);

            if (registeredPackets.containsKey(type)) {
                OELib.LOGGER.warn("Packet {} is already registered, skipping", type.id());
                return;
            }

            PacketInfo<T> info = new PacketInfo<>(type, codec);
            registeredPackets.put(type, info);

            PayloadTypeRegistry.playC2S().register(type, codec);
            PayloadTypeRegistry.playS2C().register(type, codec);

            ServerPlayNetworking.registerGlobalReceiver(type, (packet, context) -> {
                context.server().execute(() -> {
                    FabricNetworkContext networkContext = new FabricNetworkContext(context.player(), true);
                    packet.handle(networkContext);
                });
            });

            OELib.LOGGER.info("Registered network packet: {} (ID: {})",
                    type.id().getPath(), type.id());

        } catch (Exception e) {
            throw new RuntimeException("Failed to register packet " + packetClass.getSimpleName(), e);
        }
    }

    /**
     * Returns the set of registered packet types.
     *
     * @return registered packet types
     */
    public static Set<CustomPacketPayload.Type<?>> getRegisteredPacketTypes() {
        return new HashSet<>(registeredPackets.keySet());
    }

    private static void registerBuiltinPackets() {
        NetworkManagerImpl impl = new NetworkManagerImpl();
        for (Class<? extends INetworkPacket<?>> rawClass : NetworkAutoRegistration.findAllAnnotatedPackets()) {
            impl.registerServerAnnotatedPacket(rawClass);
        }
        OELib.LOGGER.info("Registered builtin server network packets");
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet) {
        try {
            ClientPlayNetworking.send(packet);
            OELib.LOGGER.debug("Sent packet {} to server", packet.type().id());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to server: {}",
                    packet.type().id(), e.getMessage(), e);
        }
    }

    public static void initializeClient() {
        NetworkManagerImpl impl = new NetworkManagerImpl();
        for (Class<? extends INetworkPacket<?>> rawClass : NetworkAutoRegistration.findAllAnnotatedPackets()) {
            impl.registerClientAnnotatedPacket(rawClass);
        }
        OELib.LOGGER.info("Registered builtin client network packets");
    }

    /**
     * Creates a packet type descriptor using {@link NetworkPacket} metadata.
     */
    private <T extends INetworkPacket<T> & CustomPacketPayload> CustomPacketPayload.Type<T> createPacketType(Class<T> packetClass) {
        return NetworkPacketTypes.typeOf(packetClass);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player) {
        if (player == null) {
            OELib.LOGGER.warn("Cannot send packet {}: player is null", packet.type().id());
            return;
        }

        try {
            Class<?> packetClass = packet.getClass();
            int threshold = 0;
            if (packetClass.isAnnotationPresent(NetworkPacket.class)) {
                threshold = packetClass.getAnnotation(NetworkPacket.class).chunkThreshold();
            }
            if (threshold > 0) {
                sendWithChunking(packet, Collections.singletonList(player), threshold);
            } else {
                ServerPlayNetworking.send(player, packet);
                OELib.LOGGER.debug("Sent packet {} to player {}",
                        packet.type().id(), player.getName().getString());
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to player {}: {}",
                    packet.type().id(), player.getName().getString(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAll(T packet) {
        try {
            MinecraftServer server = DataManager.getCurrentServer();
            if (server != null) {
                Class<?> packetClass = packet.getClass();
                int threshold = 0;
                if (packetClass.isAnnotationPresent(NetworkPacket.class)) {
                    threshold = packetClass.getAnnotation(NetworkPacket.class).chunkThreshold();
                }
                if (threshold > 0) {
                    List<ServerPlayer> players = new ArrayList<>(PlayerLookup.all(server));
                    sendWithChunking(packet, players, threshold);
                } else {
                    for (ServerPlayer player : PlayerLookup.all(server)) {
                        ServerPlayNetworking.send(player, packet);
                    }
                }
            }
            OELib.LOGGER.debug("Sent packet {} to all players", packet.type().id());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to all players: {}",
                    packet.type().id(), e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void sendWithChunking(
            T packet, Iterable<ServerPlayer> players, int chunkThreshold) {
        try {
            RegistryFriendlyByteBuf tempBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);

            PacketInfo<T> info = (PacketInfo<T>) registeredPackets.get(packet.type());
            if (info != null) {
                info.codec().encode(tempBuf, packet);

                byte[] packetData = new byte[tempBuf.readableBytes()];
                tempBuf.readBytes(packetData);
                tempBuf.release();

                if (packetData.length <= chunkThreshold) {
                    for (ServerPlayer player : players) {
                        ServerPlayNetworking.send(player, packet);
                    }
                    OELib.LOGGER.debug("Sent packet {} without chunking ({} bytes)",
                            packet.type().id(), packetData.length);
                } else {
                    sendChunkedPacket(packetData, packet.getClass().getName(), players, chunkThreshold);
                }
            } else {
                OELib.LOGGER.error("No codec found for packet type: {}", packet.type().id());
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} with chunking: {}",
                    packet.type().id(), e.getMessage(), e);
        }
    }

    /**
     * Sends a chunked data packet.
     */
    private void sendChunkedPacket(byte[] data, String packetClassName, Iterable<ServerPlayer> players, int chunkSize) {
        try {
            UUID sessionId = UUID.randomUUID();
            int totalChunks = (int) Math.ceil((double) data.length / chunkSize);

            OELib.LOGGER.info("Splitting {} packet into {} chunks for session {} ({} bytes total)",
                    packetClassName, totalChunks, sessionId, data.length);

            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, data.length);
                int currentChunkSize = end - start;

                byte[] chunkData = new byte[currentChunkSize];
                System.arraycopy(data, start, chunkData, 0, currentChunkSize);

                DataSyncChunkPacket chunk = new DataSyncChunkPacket(
                        sessionId, i, totalChunks, packetClassName, chunkData);

                for (ServerPlayer player : players) {
                    ServerPlayNetworking.send(player, chunk);
                }

                OELib.LOGGER.debug("Sent chunk {}/{} ({} bytes) for {} session {}",
                        i + 1, totalChunks, currentChunkSize, packetClassName, sessionId);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send chunked packet {}: {}", packetClassName, e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerAnnotatedPacket(
            Class<? extends INetworkPacket<?>> rawClass,
            BiConsumer<Side, RegistrationContext<T>> registrationLogic) {

        Class<T> packetClass = (Class<T>) rawClass;
        NetworkPacket meta = packetClass.getAnnotation(NetworkPacket.class);

        if (meta == null) return;

        if (!packetClass.isRecord()) {
            OELib.LOGGER.warn("Skipping non-record network packet {}", packetClass.getName());
            return;
        }

        StreamCodec<RegistryFriendlyByteBuf, T> codec = NetworkSerialization.autoCodec(packetClass);

        registrationLogic.accept(meta.side(), new RegistrationContext<>(packetClass, codec));
    }

    private void registerServerAnnotatedPacket(Class<? extends INetworkPacket<?>> rawClass) {
        registerAnnotatedPacket(rawClass, (side, ctx) -> {
            switch (side) {
                case SERVER -> registerServerPacket(ctx.packetClass, ctx.codec);
                case BOTH -> registerBidirectionalPacket(ctx.packetClass, ctx.codec);
            }
        });
    }

    private void registerClientAnnotatedPacket(Class<? extends INetworkPacket<?>> rawClass) {
        registerAnnotatedPacket(rawClass, (side, ctx) -> {
            if (side == Side.CLIENT || side == Side.BOTH) {
                registerClientPacket(ctx.packetClass, ctx.codec);
            }
        });
    }

    private record RegistrationContext<T>(Class<T> packetClass, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
    }

    private record PacketInfo<T extends INetworkPacket<T> & CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
    }
}