package com.sighs.oelib.fabric.network;

import com.sighs.oelib.OELib;
import com.sighs.oelib.data.net.DataSyncChunkPacket;
import com.sighs.oelib.fabric.data.DataManager;
import com.sighs.oelib.network.api.INetworkManager;
import com.sighs.oelib.network.api.INetworkPacket;
import com.sighs.oelib.network.api.NetworkPacket;
import com.sighs.oelib.network.api.Side;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Fabric网络管理器。
 * <p>
 * 提供统一的网络包注册和发送功能。
 * </p>
 */
public class NetworkManager implements INetworkManager {

    private static final Map<Class<?>, PacketInfo<?>> registeredPackets = new ConcurrentHashMap<>();
    private static NetworkManager instance;

    /**
     * 初始化网络管理器。
     * <p>
     * 此方法应该在模组初始化时调用。
     * </p>
     */
    public static void initialize() {
        instance = new NetworkManager();
        com.sighs.oelib.network.api.NetworkManager.setInstance(instance);

        // 注册内置的数据同步包
        registerBuiltinPackets();

        OELib.LOGGER.info("Network manager initialized");
    }

    /**
     * 注册单个网络包（无类型检查版本）。
     *
     * @param packetClass 网络包类
     */
    @SuppressWarnings("unchecked")
    private static void registerPacketUnchecked(Class<? extends INetworkPacket<?>> packetClass) {
        registerPacket((Class<? extends INetworkPacket>) packetClass);
    }

    /**
     * 注册单个网络包。
     *
     * @param packetClass 网络包类
     * @param <T>         网络包类型
     */
    public static <T extends INetworkPacket<T>> void registerPacket(Class<T> packetClass) {
        if (!packetClass.isAnnotationPresent(NetworkPacket.class)) {
            throw new IllegalArgumentException("Class " + packetClass.getSimpleName() + " must be annotated with @NetworkPacket");
        }

        if (registeredPackets.containsKey(packetClass)) {
            OELib.LOGGER.warn("Packet {} is already registered, skipping", packetClass.getSimpleName());
            return;
        }

        try {
            Function<FriendlyByteBuf, T> decoder = createDecoder(packetClass);

            PacketInfo<T> info = new PacketInfo<>(packetClass, decoder);
            registeredPackets.put(packetClass, info);

            ResourceLocation id = new ResourceLocation(OELib.MODID, packetClass.getSimpleName().toLowerCase());
            NetworkPacket annotation = packetClass.getAnnotation(NetworkPacket.class);
            Side side = annotation.side();

            // 根据 side 参数选择性注册接收器
            if (side == Side.SERVER || side == Side.BOTH) {
                registerServerReceiver(id, decoder);
            }

            int chunkThreshold = annotation.chunkThreshold();

            OELib.LOGGER.info("Registered network packet: {} (ID: {}, Side: {}, Chunk Threshold: {} bytes)",
                    packetClass.getSimpleName(), id, side,
                    chunkThreshold > 0 ? chunkThreshold : "No chunking");

        } catch (Exception e) {
            throw new RuntimeException("Failed to register packet " + packetClass.getSimpleName(), e);
        }
    }

    /**
     * 注册客户端网络包接收器。
     * 此方法只能在客户端环境中调用。
     *
     * @param packetClass 网络包类
     * @param <T>         网络包类型
     */
    public static <T extends INetworkPacket<T>> void registerClientPacket(Class<T> packetClass) {
        if (!packetClass.isAnnotationPresent(NetworkPacket.class)) {
            throw new IllegalArgumentException("Class " + packetClass.getSimpleName() + " must be annotated with @NetworkPacket");
        }

        try {
            Function<FriendlyByteBuf, T> decoder = createDecoder(packetClass);

            ResourceLocation id = new ResourceLocation(OELib.MODID, packetClass.getSimpleName().toLowerCase());
            NetworkPacket annotation = packetClass.getAnnotation(NetworkPacket.class);
            Side side = annotation.side();

            if (side == Side.CLIENT || side == Side.BOTH) {
                registerClientReceiver(id, decoder);
            }

            OELib.LOGGER.info("Registered client network packet receiver: {} (ID: {})",
                    packetClass.getSimpleName(), id);

        } catch (Exception e) {
            throw new RuntimeException("Failed to register client packet " + packetClass.getSimpleName(), e);
        }
    }

    /**
     * 创建网络包的解码函数。
     *
     * @param packetClass 网络包类
     * @param <T>         网络包类型
     * @return 解码函数，将 FriendlyByteBuf 转为网络包实例
     * @throws NoSuchMethodException 如果找不到 decode 方法
     */
    @SuppressWarnings("unchecked")
    private static <T extends INetworkPacket<T>> Function<FriendlyByteBuf, T> createDecoder(Class<T> packetClass) throws NoSuchMethodException {
        // 查找 decode 方法
        Method decodeMethod = packetClass.getDeclaredMethod("decode", FriendlyByteBuf.class);
        decodeMethod.setAccessible(true);

        // 构造解码函数
        return buf -> {
            try {
                return (T) decodeMethod.invoke(null, buf);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode packet " + packetClass.getSimpleName(), e);
            }
        };
    }

    /**
     * 注册服务端接收器。
     */
    private static <T extends INetworkPacket<T>> void registerServerReceiver(ResourceLocation id, Function<FriendlyByteBuf, T> decoder) {
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, responseSender) -> {
            T packet = decoder.apply(buf);
            server.execute(() -> {
                FabricNetworkContext context = new FabricNetworkContext(player, true);
                packet.handle(context);
            });
        });
    }

    /**
     * 注册客户端接收器。
     */
    private static <T extends INetworkPacket<T>> void registerClientReceiver(ResourceLocation id, Function<FriendlyByteBuf, T> decoder) {
        ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, responseSender) -> {
            T packet = decoder.apply(buf);
            client.execute(() -> {
                FabricNetworkContext context = new FabricNetworkContext(null, false);
                packet.handle(context);
            });
        });
    }

    /**
     * 使用分片发送网络包。
     */
    private static <T extends INetworkPacket<T>> void sendWithChunking(T packet, Iterable<ServerPlayer> players, int chunkThreshold) {
        try {
            // 将包编码为字节数组
            FriendlyByteBuf tempBuf = new FriendlyByteBuf(Unpooled.buffer());
            packet.encode(tempBuf);
            byte[] packetData = new byte[tempBuf.readableBytes()];
            tempBuf.readBytes(packetData);
            tempBuf.release();

            if (packetData.length <= chunkThreshold) {
                // 不需要分片，直接发送
                for (ServerPlayer player : players) {
                    instance.sendToPlayer(packet, player);
                }
                OELib.LOGGER.debug("Sent packet {} without chunking ({} bytes)",
                        packet.getClass().getSimpleName(), packetData.length);
            } else {
                // 需要分片发送
                sendChunkedPacket(packetData, packet.getClass().getName(), players, chunkThreshold);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} with chunking: {}",
                    packet.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * 发送分片数据包。
     */
    private static void sendChunkedPacket(byte[] data, String packetClassName, Iterable<ServerPlayer> players, int chunkSize) {
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
                    NetworkHandler.sendTo(player, chunk);
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
     * 获取已注册的网络包类列表。
     *
     * @return 已注册的网络包类列表
     */
    public static Set<Class<?>> getRegisteredPacketClasses() {
        return new HashSet<>(registeredPackets.keySet());
    }

    private static void registerBuiltinPackets() {
        registerPacket(DataSyncChunkPacket.class);

        OELib.LOGGER.info("Registered builtin data sync packets");
    }

    @Override
    @SafeVarargs
    public final void registerPackets(Class<? extends INetworkPacket<?>>... packetClasses) {
        List<Class<? extends INetworkPacket<?>>> sortedClasses = Arrays.asList(packetClasses);

        // 按优先级排序
        sortedClasses.sort((a, b) -> {
            NetworkPacket annotationA = a.getAnnotation(NetworkPacket.class);
            NetworkPacket annotationB = b.getAnnotation(NetworkPacket.class);

            int priorityA = annotationA != null ? annotationA.priority() : 1000;
            int priorityB = annotationB != null ? annotationB.priority() : 1000;

            return Integer.compare(priorityA, priorityB);
        });

        for (Class<? extends INetworkPacket<?>> packetClass : sortedClasses) {
            registerPacketUnchecked(packetClass);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToPlayer(T packet, ServerPlayer player) {
        if (player == null) {
            OELib.LOGGER.warn("Cannot send packet {}: player is null", packet.getClass().getSimpleName());
            return;
        }

        try {
            ResourceLocation id = new ResourceLocation(OELib.MODID, packet.getClass().getSimpleName().toLowerCase());
            FriendlyByteBuf buf = PacketByteBufs.create();
            packet.encode(buf);
            ServerPlayNetworking.send(player, id, buf);
            OELib.LOGGER.debug("Sent packet {} to player {}",
                    packet.getClass().getSimpleName(), player.getName().getString());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to player {}: {}",
                    packet.getClass().getSimpleName(), player.getName().getString(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToAll(T packet) {
        try {
            ResourceLocation id = new ResourceLocation(OELib.MODID, packet.getClass().getSimpleName().toLowerCase());
            MinecraftServer server = DataManager.getCurrentServer();
            if (server != null) {
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    packet.encode(buf);
                    ServerPlayNetworking.send(player, id, buf);
                }
            }
            OELib.LOGGER.debug("Sent packet {} to all players", packet.getClass().getSimpleName());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to all players: {}",
                    packet.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToServer(T packet) {
        try {
            ResourceLocation id = new ResourceLocation(OELib.MODID, packet.getClass().getSimpleName().toLowerCase());
            FriendlyByteBuf buf = PacketByteBufs.create();
            packet.encode(buf);
            ClientPlayNetworking.send(id, buf);
            OELib.LOGGER.debug("Sent packet {} to server", packet.getClass().getSimpleName());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to server: {}",
                    packet.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToPlayerWithChunking(T packet, ServerPlayer player) {
        if (player == null) {
            OELib.LOGGER.warn("Cannot send packet {}: player is null", packet.getClass().getSimpleName());
            return;
        }

        NetworkPacket annotation = packet.getClass().getAnnotation(NetworkPacket.class);
        if (annotation != null && annotation.chunkThreshold() > 0) {
            sendWithChunking(packet, Collections.singletonList(player), annotation.chunkThreshold());
        } else {
            sendToPlayer(packet, player);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToAllWithChunking(T packet) {
        NetworkPacket annotation = packet.getClass().getAnnotation(NetworkPacket.class);
        if (annotation != null && annotation.chunkThreshold() > 0) {
            MinecraftServer server = DataManager.getCurrentServer();
            if (server != null) {
                sendWithChunking(packet, PlayerLookup.all(server), annotation.chunkThreshold());
            }
        } else {
            sendToAll(packet);
        }
    }

    private record PacketInfo<T extends INetworkPacket<T>>(Class<T> packetClass, Function<FriendlyByteBuf, T> decoder) {
    }
}