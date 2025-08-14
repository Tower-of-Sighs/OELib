package com.mafuyu404.oelib.fabric.network;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.api.net.INetworkManager;
import com.mafuyu404.oelib.api.net.INetworkPacket;
import com.mafuyu404.oelib.api.net.NetworkPacket;
import com.mafuyu404.oelib.api.net.SimplePacket;
import com.mafuyu404.oelib.fabric.data.DataManager;
import com.mafuyu404.oelib.fabric.data.net.DataSyncChunkPacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric网络管理器。
 * <p>
 * 提供统一的网络包注册和发送功能。
 * </p>
 */
public class NetworkManager implements INetworkManager {

    private static final Map<CustomPacketPayload.Type<?>, PacketInfo<?>> registeredPackets = new ConcurrentHashMap<>();
    private static NetworkManager instance;

    /**
     * 初始化网络管理器。
     * <p>
     * 此方法应该在模组初始化时调用。
     * </p>
     */
    public static void initialize() {
        instance = new NetworkManager();
        com.mafuyu404.oelib.api.net.NetworkManager.setInstance(instance);

        // 注册内置的数据同步包
        registerBuiltinPackets();

        OELib.LOGGER.info("Network manager initialized");
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void registerBidirectionalPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        registerServerOrBidirectionalPacket(packetClass, codec);
    }

    @Override
    public void registerBidirectionalPackets(PacketRegistration<?>... packets) {
        for (PacketRegistration<?> packet : packets) {
            registerBidirectionalPacketUnchecked(packet.packetClass(), packet.codec());
        }
    }

    @Override
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

    @Override
    public void registerClientPackets(PacketRegistration<?>... packets) {
        for (PacketRegistration<?> packet : packets) {
            registerClientPacketUnchecked(packet.packetClass(), packet.codec());
        }
    }


    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void registerServerPacket(Class<T> packetClass, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        registerServerOrBidirectionalPacket(packetClass, codec);
    }

    @Override
    public void registerServerPackets(PacketRegistration<?>... packets) {
        for (PacketRegistration<?> packet : packets) {
            registerServerPacketUnchecked(packet.packetClass(), packet.codec());
        }
    }

    /**
     * 通过类名创建网络包类型。
     */
    private <T extends INetworkPacket<T> & CustomPacketPayload> CustomPacketPayload.Type<T> createPacketType(Class<T> packetClass) {
        try {
            // 尝试获取模组ID
            String modId = getModIdFromClass(packetClass);
            String className = packetClass.getSimpleName().toLowerCase();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modId, className);
            return new CustomPacketPayload.Type<>(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create packet type for " + packetClass.getSimpleName(), e);
        }
    }

    /**
     * 从类中获取模组ID。
     */
    private String getModIdFromClass(Class<?> packetClass) {
        try {
            // 尝试创建临时实例来获取模组ID
            Object tempInstance = packetClass.getDeclaredConstructor().newInstance();
            if (tempInstance instanceof SimplePacket) {
                return ((SimplePacket<?>) tempInstance).getModId();
            }
        } catch (Exception e) {
            // 如果无法创建实例，尝试从包名推断
            String packageName = packetClass.getPackage().getName();
            if (packageName.contains("oelib")) {
                return "oelib";
            }
        }

        return "oelib";
    }

    private  <T extends INetworkPacket<T> & CustomPacketPayload> void registerServerOrBidirectionalPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        try {
            // 通过类名生成类型信息，避免创建临时实例
            CustomPacketPayload.Type<T> type = createPacketType(packetClass);

            if (registeredPackets.containsKey(type)) {
                OELib.LOGGER.warn("Packet {} is already registered, skipping", type.id());
                return;
            }

            PacketInfo<T> info = new PacketInfo<>(type, codec);
            registeredPackets.put(type, info);

            PayloadTypeRegistry.playC2S().register(type, codec);
            PayloadTypeRegistry.playS2C().register(type, codec);

            // 注册服务端接收器
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
     * 注册单个双端网络包（无类型检查版本）。
     */
    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerBidirectionalPacketUnchecked(
            Class<?> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, ?> codec
    ) {
        registerBidirectionalPacket((Class<T>) packetClass, (StreamCodec<? super RegistryFriendlyByteBuf, T>) codec);
    }

    /**
     * 注册单个客户端网络包（无类型检查版本）。
     */
    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerClientPacketUnchecked(
            Class<?> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, ?> codec
    ) {
        registerClientPacket((Class<T>) packetClass, (StreamCodec<? super RegistryFriendlyByteBuf, T>) codec);
    }

    /**
     * 注册单个服务端端网络包（无类型检查版本）。
     */
    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerServerPacketUnchecked(
            Class<?> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, ?> codec
    ) {
        registerServerPacket((Class<T>) packetClass, (StreamCodec<? super RegistryFriendlyByteBuf, T>) codec);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player) {
        if (player == null) {
            OELib.LOGGER.warn("Cannot send packet {}: player is null", packet.type().id());
            return;
        }

        try {
            ServerPlayNetworking.send(player, packet);
            OELib.LOGGER.debug("Sent packet {} to player {}",
                    packet.type().id(), player.getName().getString());
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
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    ServerPlayNetworking.send(player, packet);
                }
            }
            OELib.LOGGER.debug("Sent packet {} to all players", packet.type().id());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to all players: {}",
                    packet.type().id(), e.getMessage(), e);
        }
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

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayerWithChunking(T packet, ServerPlayer player) {
        if (player == null) {
            OELib.LOGGER.warn("Cannot send packet {}: player is null", packet.type().id());
            return;
        }

        // 检查是否需要分片
        Class<?> packetClass = packet.getClass();
        if (packetClass.isAnnotationPresent(NetworkPacket.class)) {
            NetworkPacket annotation = packetClass.getAnnotation(NetworkPacket.class);
            if (annotation.chunkThreshold() > 0) {
                sendWithChunking(packet, Collections.singletonList(player), annotation.chunkThreshold());
                return;
            }
        }

        sendToPlayer(packet, player);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAllWithChunking(T packet) {
        // 检查是否需要分片
        Class<?> packetClass = packet.getClass();
        if (packetClass.isAnnotationPresent(NetworkPacket.class)) {
            NetworkPacket annotation = packetClass.getAnnotation(NetworkPacket.class);
            if (annotation.chunkThreshold() > 0) {
                MinecraftServer server = DataManager.getCurrentServer();
                if (server != null) {
                    List<ServerPlayer> players = new ArrayList<>(PlayerLookup.all(server));
                    sendWithChunking(packet, players, annotation.chunkThreshold());
                    return;
                }
            }
        }

        sendToAll(packet);
    }

    /**
     * 使用分片发送网络包。
     */
    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void sendWithChunking(
            T packet, Iterable<ServerPlayer> players, int chunkThreshold) {
        try {
            // 将包编码为字节数组
            RegistryFriendlyByteBuf tempBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);

            // 获取对应的编解码器并编码
            PacketInfo<T> info = (PacketInfo<T>) registeredPackets.get(packet.type());
            if (info != null) {
                info.codec().encode(tempBuf, packet);

                byte[] packetData = new byte[tempBuf.readableBytes()];
                tempBuf.readBytes(packetData);
                tempBuf.release();

                if (packetData.length <= chunkThreshold) {
                    // 不需要分片，直接发送
                    for (ServerPlayer player : players) {
                        sendToPlayer(packet, player);
                    }
                    OELib.LOGGER.debug("Sent packet {} without chunking ({} bytes)",
                            packet.type().id(), packetData.length);
                } else {
                    // 需要分片发送
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
     * 发送分片数据包。
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

    /**
     * 获取已注册的网络包数量。
     *
     * @return 已注册的网络包数量
     */
    public static int getRegisteredPacketCount() {
        return registeredPackets.size();
    }

    /**
     * 获取已注册的网络包类型列表。
     *
     * @return 已注册的网络包类型列表
     */
    public static Set<CustomPacketPayload.Type<?>> getRegisteredPacketTypes() {
        return new HashSet<>(registeredPackets.keySet());
    }

    private static void registerBuiltinPackets() {
        instance.registerServerPacket(DataSyncChunkPacket.class, DataSyncChunkPacket.STREAM_CODEC);

        OELib.LOGGER.info("Registered builtin data sync packets");
    }

    private record PacketInfo<T extends INetworkPacket<T> & CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {}
}