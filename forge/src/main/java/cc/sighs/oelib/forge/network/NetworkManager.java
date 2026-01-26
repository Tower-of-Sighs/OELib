package cc.sighs.oelib.forge.network;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.data.net.DataSyncChunkPacket;
import cc.sighs.oelib.network.api.INetworkManager;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModLoadingContext;
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
    private static final Map<String, ChannelContext> channels = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String> packetOwnerMod = new ConcurrentHashMap<>();
    private static NetworkManager instance;

    /**
     * 初始化网络管理器。
     * <p>
     * 此方法应该在模组初始化时调用。
     * </p>
     */
    public static void initialize() {
        // 确保创建 OELib 的默认 channel
        ensureChannel(OELib.MODID);

        instance = new NetworkManager();
        cc.sighs.oelib.network.api.NetworkManager.setInstance(instance);

        // 注册内置的数据同步包到 OELib 默认通道
        registerBuiltinPackets();

        OELib.LOGGER.info("Network manager initialized");
    }

    // 创建或获取指定 modid 的 channel，并在首次创建时注册内置分片包
    private static ChannelContext ensureChannel(String modid) {
        return channels.computeIfAbsent(modid, id -> {
            SimpleChannel ch = NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(id, "main"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );
            ChannelContext ctx = new ChannelContext(ch);

            // 确保每个通道都具备分片能力
            // 注意：这里不走外部排序，直接在该通道内先注册 DataSyncChunkPacket
            internalRegisterPacket(ctx, id, DataSyncChunkPacket.class);

            OELib.LOGGER.info("Created SimpleChannel for mod {}: {}", id, new ResourceLocation(id, "main"));
            return ctx;
        });
    }

    // 获取当前调用方的 modid，若不可用则回退到 OELib.MODID
    private static String currentModIdOrDefault() {
        try {
            var container = ModLoadingContext.get().getActiveContainer();
            if (container != null) {
                String id = container.getModId();
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
        } catch (Throwable ignored) {
        }
        return OELib.MODID;
    }

    /**
     * 注册单个网络包（无类型检查版本）。
     *
     * @param packetClass 网络包类
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerPacketUnchecked(ChannelContext ctx, String modid, Class<? extends INetworkPacket<?>> packetClass) {
        internalRegisterPacket(ctx, modid, (Class<? extends INetworkPacket>) packetClass);
    }

    /**
     * 注册单个网络包。
     *
     * @param packetClass 网络包类
     * @param <T>         网络包类型
     */
    public static <T extends INetworkPacket<T>> void registerPacket(Class<T> packetClass) {
        ChannelContext ctx = ensureChannel(OELib.MODID);
        internalRegisterPacket(ctx, OELib.MODID, packetClass);
    }

    // 在指定 ctx/modid 的通道里注册
    @SuppressWarnings("unchecked")
    private static <T extends INetworkPacket<T>> void internalRegisterPacket(ChannelContext ctx, String modid, Class<T> packetClass) {
        if (!packetClass.isAnnotationPresent(NetworkPacket.class)) {
            throw new IllegalArgumentException("Class " + packetClass.getSimpleName() + " must be annotated with @NetworkPacket");
        }

        if (ctx.packets.containsKey(packetClass)) {
            OELib.LOGGER.warn("Packet {} is already registered for mod {}, skipping", packetClass.getSimpleName(), modid);
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
            ctx.packets.put(packetClass, info);
            registeredPackets.put(packetClass, info);
            packetOwnerMod.put(packetClass, modid);

            NetworkPacket annotation = packetClass.getAnnotation(NetworkPacket.class);
            Side side = annotation.side();

            // 根据 Side 枚举确定 NetworkDirection
            Optional<NetworkDirection> networkDirection = getNetworkDirection(side);

            ctx.channel.registerMessage(
                    ctx.nextPacketId++,
                    packetClass,
                    INetworkPacket::encode,
                    decoder,
                    (packet, c) -> {
                        c.get().enqueueWork(() -> {
                            ForgeNetworkContext context = new ForgeNetworkContext(c.get());
                            packet.handle(context);
                        });
                        c.get().setPacketHandled(true);
                    },
                    networkDirection
            );

            int chunkThreshold = annotation.chunkThreshold();

            OELib.LOGGER.info("Registered network packet: {} (mod: {}, ID: {}, Side: {}, NetworkDirection: {}, Chunk Threshold: {} bytes)",
                    packetClass.getSimpleName(), modid, ctx.nextPacketId - 1, side,
                    networkDirection.map(Enum::name).orElse("BOTH"),
                    chunkThreshold > 0 ? chunkThreshold : "No chunking");

        } catch (Exception e) {
            throw new RuntimeException("Failed to register packet " + packetClass.getSimpleName() + " for mod " + modid, e);
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

    // 根据包类定位对应通道；若找不到则回退到 OELib 通道并警告
    private static SimpleChannel resolveChannelForPacket(Class<?> packetClass) {
        String modid = packetOwnerMod.get(packetClass);
        if (modid != null) {
            ChannelContext ctx = channels.get(modid);
            if (ctx != null) return ctx.channel;
        }
        OELib.LOGGER.warn("Packet {} has no owner channel, fallback to OELib default channel", packetClass.getSimpleName());
        return ensureChannel(OELib.MODID).channel;
    }

    /**
     * 使用分片发送网络包。
     */
    private static <T extends INetworkPacket<T>> void sendWithChunking(SimpleChannel ch, T packet, PacketDistributor.PacketTarget target, int chunkThreshold) {
        try {
            // 将包编码为字节数组
            FriendlyByteBuf tempBuf = new FriendlyByteBuf(Unpooled.buffer());
            packet.encode(tempBuf);
            byte[] packetData = new byte[tempBuf.readableBytes()];
            tempBuf.readBytes(packetData);
            tempBuf.release();

            if (packetData.length <= chunkThreshold) {
                // 不需要分片，直接发送
                ch.send(target, packet);
                OELib.LOGGER.debug("Sent packet {} without chunking ({} bytes)",
                        packet.getClass().getSimpleName(), packetData.length);
            } else {
                // 需要分片发送（在同一 channel 上发送分片包）
                sendChunkedPacket(ch, packetData, packet.getClass().getName(), target, chunkThreshold);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} with chunking: {}",
                    packet.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * 发送分片数据包（通过对应的 channel）。
     */
    private static void sendChunkedPacket(SimpleChannel ch, byte[] data, String packetClassName, PacketDistributor.PacketTarget target, int chunkSize) {
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
                ch.send(target, chunk);

                OELib.LOGGER.debug("Sent chunk {}/{} ({} bytes) for {} session {}",
                        i + 1, totalChunks, currentChunkSize, packetClassName, sessionId);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send chunked packet {}: {}", packetClassName, e.getMessage(), e);
        }
    }

    /**
     * 获取已注册的网络包数量（所有通道总计）。
     *
     * @return 已注册的网络包数量
     */
    public static int getRegisteredPacketCount() {
        return registeredPackets.size();
    }

    /**
     * 获取已注册的网络包类列表（所有通道总计）。
     *
     * @return 已注册的网络包类列表
     */
    public static Set<Class<?>> getRegisteredPacketClasses() {
        return new HashSet<>(registeredPackets.keySet());
    }

    /**
     * 获取网络通道实例（OELib 默认通道）。
     * <p>
     * 此方法主要用于内部使用和高级用户。
     * </p>
     *
     * @return OELib 默认通道实例
     */
    public static SimpleChannel getChannel() {
        return ensureChannel(OELib.MODID).channel;
    }

    private static void registerBuiltinPackets() {
        // 仅在 OELib 通道注册一次；其他通道在 ensureChannel 时已自动注册
        internalRegisterPacket(ensureChannel(OELib.MODID), OELib.MODID, DataSyncChunkPacket.class);

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

        // 自动根据当前 active container 分配到对应的通道
        String modid = currentModIdOrDefault();
        ChannelContext ctx = ensureChannel(modid);

        for (Class<? extends INetworkPacket<?>> packetClass : sortedClasses) {
            registerPacketUnchecked(ctx, modid, packetClass);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToPlayer(T packet, ServerPlayer player) {
        SimpleChannel ch = resolveChannelForPacket(packet.getClass());
        if (ch == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        if (player == null) {
            OELib.LOGGER.warn("Cannot send packet {}: player is null", packet.getClass().getSimpleName());
            return;
        }

        try {
            ch.send(PacketDistributor.PLAYER.with(() -> player), packet);
            OELib.LOGGER.debug("Sent packet {} to player {}",
                    packet.getClass().getSimpleName(), player.getName().getString());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to player {}: {}",
                    packet.getClass().getSimpleName(), player.getName().getString(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToAll(T packet) {
        SimpleChannel ch = resolveChannelForPacket(packet.getClass());
        if (ch == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        try {
            ch.send(PacketDistributor.ALL.noArg(), packet);
            OELib.LOGGER.debug("Sent packet {} to all players", packet.getClass().getSimpleName());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to all players: {}",
                    packet.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToServer(T packet) {
        SimpleChannel ch = resolveChannelForPacket(packet.getClass());
        if (ch == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        try {
            ch.sendToServer(packet);
            OELib.LOGGER.debug("Sent packet {} to server", packet.getClass().getSimpleName());
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to send packet {} to server: {}",
                    packet.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToPlayerWithChunking(T packet, ServerPlayer player) {
        SimpleChannel ch = resolveChannelForPacket(packet.getClass());
        if (ch == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        if (player == null) {
            OELib.LOGGER.warn("Cannot send packet {}: player is null", packet.getClass().getSimpleName());
            return;
        }

        NetworkPacket annotation = packet.getClass().getAnnotation(NetworkPacket.class);
        if (annotation != null && annotation.chunkThreshold() > 0) {
            sendWithChunking(ch, packet, PacketDistributor.PLAYER.with(() -> player), annotation.chunkThreshold());
        } else {
            sendToPlayer(packet, player);
        }
    }

    @Override
    public <T extends INetworkPacket<T>> void sendToAllWithChunking(T packet) {
        SimpleChannel ch = resolveChannelForPacket(packet.getClass());
        if (ch == null) {
            throw new IllegalStateException("Network manager not initialized");
        }

        NetworkPacket annotation = packet.getClass().getAnnotation(NetworkPacket.class);
        if (annotation != null && annotation.chunkThreshold() > 0) {
            sendWithChunking(ch, packet, PacketDistributor.ALL.noArg(), annotation.chunkThreshold());
        } else {
            sendToAll(packet);
        }
    }

    // 每个 mod 的通道上下文
    private static final class ChannelContext {
        final SimpleChannel channel;
        final Map<Class<?>, PacketInfo<?>> packets = new ConcurrentHashMap<>();
        int nextPacketId = 0;

        ChannelContext(SimpleChannel channel) {
            this.channel = channel;
        }
    }

    private record PacketInfo<T extends INetworkPacket<T>>(Class<T> packetClass, Function<FriendlyByteBuf, T> decoder) {
    }
}