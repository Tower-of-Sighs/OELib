package com.mafuyu404.oelib.forge.network;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.api.net.INetworkManager;
import com.mafuyu404.oelib.api.net.INetworkPacket;
import com.mafuyu404.oelib.api.net.NetworkPacket;
import com.mafuyu404.oelib.api.net.Side;
import com.mafuyu404.oelib.forge.data.net.DataSyncChunkPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Forge网络管理器。
 * <p>
 * 提供统一的网络包注册和发送功能。
 * </p>
 */
public class NetworkManager implements INetworkManager {

    private static final String PROTOCOL_VERSION = "1";
    private static final Map<Class<?>, PacketInfo<?>> registeredPackets = new ConcurrentHashMap<>();
    private static SimpleChannel channel;
    private static int nextPacketId = 0;
    private static NetworkManager instance;

    /**
     * 初始化网络管理器。
     * <p>
     * 此方法应该在模组初始化时调用。
     * </p>
     */
    public static void initialize() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(OELib.MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        instance = new NetworkManager();
        com.mafuyu404.oelib.api.net.NetworkManager.setInstance(instance);

        // 注册内置的数据同步包
        registerBuiltinPackets();

        OELib.LOGGER.info("Network manager initialized");
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
    @SuppressWarnings("unchecked")
    public static <T extends INetworkPacket<T>> void registerPacket(Class<T> packetClass) {
        if (!packetClass.isAnnotationPresent(NetworkPacket.class)) {
            throw new IllegalArgumentException("Class " + packetClass.getSimpleName() + " must be annotated with @NetworkPacket");
        }

        if (registeredPackets.containsKey(packetClass)) {
            OELib.LOGGER.warn("Packet {} is already registered, skipping", packetClass.getSimpleName());
            return;
        }

        try {
            // 查找decode方法
            Method decodeMethod = packetClass.getDeclaredMethod("decode", FriendlyByteBuf.class);
            decodeMethod.setAccessible(true);

            Function<FriendlyByteBuf, T> decoder = buf -> {
                try {
                    return (T) decodeMethod.invoke(null, buf);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decode packet " + packetClass.getSimpleName(), e);
                }
            };

            PacketInfo<T> info = new PacketInfo<>(packetClass, decoder);
            registeredPackets.put(packetClass, info);

            NetworkPacket annotation = packetClass.getAnnotation(NetworkPacket.class);
            Side side = annotation.side();

            // 根据 Side 枚举确定 NetworkDirection
            Optional<NetworkDirection> networkDirection = getNetworkDirection(side);

            channel.registerMessage(
                    nextPacketId++,
                    packetClass,
                    INetworkPacket::encode,
                    decoder,
                    (packet, ctx) -> {
                        ctx.get().enqueueWork(() -> {
                            ForgeNetworkContext context = new ForgeNetworkContext(ctx.get());
                            packet.handle(context);
                        });
                        ctx.get().setPacketHandled(true);
                    },
                    networkDirection
            );

            int chunkThreshold = annotation.chunkThreshold();

            OELib.LOGGER.info("Registered network packet: {} (ID: {}, Side: {}, NetworkDirection: {}, Chunk Threshold: {} bytes)",
                    packetClass.getSimpleName(), nextPacketId - 1, side,
                    networkDirection.map(Enum::name).orElse("BOTH"),
                    chunkThreshold > 0 ? chunkThreshold : "No chunking");

        } catch (Exception e) {
            throw new RuntimeException("Failed to register packet " + packetClass.getSimpleName(), e);
        }
    }

    /**
     * 根据 Side 枚举获取对应的 NetworkDirection。
     *
     * @param side Side 枚举值
     * @return 对应的 NetworkDirection，如果是 BOTH 则返回 Optional.empty()
     */
    private static Optional<NetworkDirection> getNetworkDirection(Side side) {
        return switch (side) {
            case CLIENT -> Optional.of(NetworkDirection.PLAY_TO_CLIENT);
            case SERVER -> Optional.of(NetworkDirection.PLAY_TO_SERVER);
            case BOTH -> Optional.empty(); // 双向通信，不指定方向
        };
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToPlayer(T packet, ServerPlayer player) {
        if (channel == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        if (player == null) {
            OELib.LOGGER.warn("Cannot send packet {}: player is null", packet.getClass().getSimpleName());
            return;
        }

        try {
            channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
            OELib.LOGGER.debug("Sent packet {} to player {}",
                    packet.getClass().getSimpleName(), player.getName().getString());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to player {}: {}",
                    packet.getClass().getSimpleName(), player.getName().getString(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToAll(T packet) {
        if (channel == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        try {
            channel.send(PacketDistributor.ALL.noArg(), packet);
            OELib.LOGGER.debug("Sent packet {} to all players", packet.getClass().getSimpleName());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to all players: {}",
                    packet.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToServer(T packet) {
        if (channel == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        try {
            channel.sendToServer(packet);
            OELib.LOGGER.debug("Sent packet {} to server", packet.getClass().getSimpleName());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to server: {}",
                    packet.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToPlayerWithChunking(T packet, ServerPlayer player) {
        if (channel == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        if (player == null) {
            OELib.LOGGER.warn("Cannot send packet {}: player is null", packet.getClass().getSimpleName());
            return;
        }

        NetworkPacket annotation = packet.getClass().getAnnotation(NetworkPacket.class);
        if (annotation != null && annotation.chunkThreshold() > 0) {
            sendWithChunking(packet, PacketDistributor.PLAYER.with(() -> player), annotation.chunkThreshold());
        } else {
            sendToPlayer(packet, player);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToAllWithChunking(T packet) {
        if (channel == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        NetworkPacket annotation = packet.getClass().getAnnotation(NetworkPacket.class);
        if (annotation != null && annotation.chunkThreshold() > 0) {
            sendWithChunking(packet, PacketDistributor.ALL.noArg(), annotation.chunkThreshold());
        } else {
            sendToAll(packet);
        }
    }

    /**
     * 使用分片发送网络包。
     */
    private static <T extends INetworkPacket<T>> void sendWithChunking(T packet, PacketDistributor.PacketTarget target, int chunkThreshold) {
        try {
            // 将包编码为字节数组
            FriendlyByteBuf tempBuf = new FriendlyByteBuf(Unpooled.buffer());
            packet.encode(tempBuf);
            byte[] packetData = new byte[tempBuf.readableBytes()];
            tempBuf.readBytes(packetData);
            tempBuf.release();

            if (packetData.length <= chunkThreshold) {
                // 不需要分片，直接发送
                channel.send(target, packet);
                OELib.LOGGER.debug("Sent packet {} without chunking ({} bytes)",
                        packet.getClass().getSimpleName(), packetData.length);
            } else {
                // 需要分片发送
                sendChunkedPacket(packetData, packet.getClass().getName(), target, chunkThreshold);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} with chunking: {}",
                    packet.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * 发送分片数据包。
     */
    private static void sendChunkedPacket(byte[] data, String packetClassName, PacketDistributor.PacketTarget target, int chunkSize) {
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
                channel.send(target, chunk);

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

    /**
     * 获取网络通道实例。
     * <p>
     * 此方法主要用于内部使用和高级用户。
     * </p>
     *
     * @return 网络通道实例
     */
    public static SimpleChannel getChannel() {
        return channel;
    }

    private static void registerBuiltinPackets() {
        registerPacket(DataSyncChunkPacket.class);

        OELib.LOGGER.info("Registered builtin data sync packets");
    }

    private record PacketInfo<T extends INetworkPacket<T>>(Class<T> packetClass, Function<FriendlyByteBuf, T> decoder) {
    }
}