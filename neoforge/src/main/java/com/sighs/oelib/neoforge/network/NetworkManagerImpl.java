package com.sighs.oelib.neoforge.network;

import com.sighs.oelib.OELib;
import com.sighs.oelib.data.net.DataSyncChunkPacket;
import com.sighs.oelib.neoforge.data.DataManager;
import com.sighs.oelib.network.api.*;
import com.sighs.oelib.network.serialization.NetworkSerialization;
import com.sighs.oelib.network.spi.INetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge implementation of the shared networking API.
 * <p>
 * Bridges {@link com.sighs.oelib.network.api.INetworkPacket} to NeoForge's
 * payload based networking system.
 * </p>
 */
@EventBusSubscriber(modid = OELib.MODID)
public class NetworkManagerImpl implements INetworkManager {

    private static final String PROTOCOL_VERSION = "1";
    private static final Map<CustomPacketPayload.Type<?>, PacketInfo<?>> registeredPackets = new ConcurrentHashMap<>();
    private static PayloadRegistrar registrar;

    /**
     * Initializes the NeoForge network manager.
     * <p>
     * This should be invoked from the payload registration event.
     * </p>
     */
    @SubscribeEvent
    public static void initialize(RegisterPayloadHandlersEvent event) {
        registrar = event.registrar(OELib.MODID).versioned(PROTOCOL_VERSION);
        registerBuiltinPackets();

        OELib.LOGGER.info("Network manager initialized");
    }

    public <T extends INetworkPacket<T> & CustomPacketPayload> void registerBidirectionalPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        if (registrar == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        try {
            // 通过类名生成类型信息，避免创建临时实例
            CustomPacketPayload.Type<T> type = createPacketType(packetClass);

            if (registeredPackets.containsKey(type)) {
                OELib.LOGGER.warn("Packet {} is already registered, skipping", type.id());
                return;
            }

            PacketInfo<T> info = new PacketInfo<>(type, codec);
            registeredPackets.put(type, info);

            // 注册双向网络包
            registrar.playBidirectional(type, codec, (packet, context) -> {
                context.enqueueWork(() -> {
                    NeoForgeNetworkContext networkContext = new NeoForgeNetworkContext(context);
                    packet.handle(networkContext);
                });
            });

            OELib.LOGGER.info("Registered network packet: {} (ID: {})",
                    type.id().getPath(), type.id());

        } catch (Exception e) {
            throw new RuntimeException("Failed to register packet " + packetClass.getSimpleName(), e);
        }
    }

    public <T extends INetworkPacket<T> & CustomPacketPayload> void registerClientPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        if (registrar == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        try {
            // 通过类名生成类型信息，避免创建临时实例
            CustomPacketPayload.Type<T> type = createPacketType(packetClass);

            // 注册客户端网络包
            registrar.playToClient(type, codec, (packet, context) -> {
                context.enqueueWork(() -> {
                    NeoForgeNetworkContext networkContext = new NeoForgeNetworkContext(context);
                    packet.handle(networkContext);
                });
            });

            OELib.LOGGER.info("Registered client network packet: {} (ID: {})",
                    type.id().getPath(), type.id());

        } catch (Exception e) {
            throw new RuntimeException("Failed to register client packet " + packetClass.getSimpleName(), e);
        }
    }

    /**
     * Returns the number of registered packet types.
     *
     * @return registered packet type count
     */
    public static int getRegisteredPacketCount() {
        return registeredPackets.size();
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
        if (registrar == null) {
            return;
        }
        NetworkManagerImpl impl = new NetworkManagerImpl();
        for (Class<? extends INetworkPacket<?>> rawClass : NetworkAutoRegistration.findAllAnnotatedPackets()) {
            impl.registerAnnotatedPacket(rawClass);
        }
        OELib.LOGGER.info("Registered builtin network packets");
    }

    /**
     * Registers a serverbound packet.
     *
     * @param packetClass packet class
     * @param codec       payload codec
     * @param <T>         packet type
     */
    public <T extends INetworkPacket<T> & CustomPacketPayload> void registerServerPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        if (registrar == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        try {
            // 通过类名生成类型信息，避免创建临时实例
            CustomPacketPayload.Type<T> type = createPacketType(packetClass);

            // 注册服务端网络包
            registrar.playToServer(type, codec, (packet, context) -> {
                context.enqueueWork(() -> {
                    NeoForgeNetworkContext networkContext = new NeoForgeNetworkContext(context);
                    packet.handle(networkContext);
                });
            });

            OELib.LOGGER.info("Registered server network packet: {} (ID: {})",
                    type.id().getPath(), type.id());

        } catch (Exception e) {
            throw new RuntimeException("Failed to register server packet " + packetClass.getSimpleName(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet) {
        try {
            PacketDistributor.sendToServer(packet);
            OELib.LOGGER.debug("Sent packet {} to server", packet.type().id());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to server: {}",
                    packet.type().id(), e.getMessage(), e);
        }
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
                PacketDistributor.sendToPlayer(player, packet);
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
            Class<?> packetClass = packet.getClass();
            int threshold = 0;
            if (packetClass.isAnnotationPresent(NetworkPacket.class)) {
                threshold = packetClass.getAnnotation(NetworkPacket.class).chunkThreshold();
            }
            if (threshold > 0) {
                MinecraftServer server = DataManager.getCurrentServer();
                if (server != null) {
                    List<ServerPlayer> players = server.getPlayerList().getPlayers();
                    sendWithChunking(packet, players, threshold);
                }
            } else {
                PacketDistributor.sendToAllPlayers(packet);
            }
            OELib.LOGGER.debug("Sent packet {} to all players", packet.type().id());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to all players: {}",
                    packet.type().id(), e.getMessage(), e);
        }
    }

    @SuppressWarnings({"unchecked", "deprecation", "DataFlowIssue"})
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
                        PacketDistributor.sendToPlayer(player, packet);
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
                    PacketDistributor.sendToPlayer(player, chunk);
                }

                OELib.LOGGER.debug("Sent chunk {}/{} ({} bytes) for {} session {}",
                        i + 1, totalChunks, currentChunkSize, packetClassName, sessionId);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send chunked packet {}: {}", packetClassName, e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerAnnotatedPacket(Class<? extends INetworkPacket<?>> rawClass) {
        Class<T> packetClass = (Class<T>) rawClass;
        NetworkPacket meta = packetClass.getAnnotation(NetworkPacket.class);
        if (meta == null) {
            return;
        }
        if (!packetClass.isRecord()) {
            OELib.LOGGER.warn("Skipping non-record network packet {}", packetClass.getName());
            return;
        }
        StreamCodec<RegistryFriendlyByteBuf, T> codec = NetworkSerialization.autoCodec(packetClass);
        Side side = meta.side();
        switch (side) {
            case CLIENT -> registerClientPacket(packetClass, codec);
            case SERVER -> registerServerPacket(packetClass, codec);
            case BOTH -> registerBidirectionalPacket(packetClass, codec);
        }
    }

    private record PacketInfo<T extends INetworkPacket<T> & CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
    }
}
