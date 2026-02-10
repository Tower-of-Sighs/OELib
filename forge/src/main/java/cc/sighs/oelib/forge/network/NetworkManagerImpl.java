package cc.sighs.oelib.forge.network;

import cc.sighs.oelib.network.api.*;
import cc.sighs.oelib.network.serialization.NetworkSerialization;
import cc.sighs.oelib.network.spi.INetworkManager;
import cc.sighs.oelib.network.util.NetworkUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class NetworkManagerImpl implements INetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    private static final String CHANNEL_NAME = "oelib_auto";
    private static final Map<String, SimpleChannel> CHANNELS = new ConcurrentHashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, NetworkUtil.PacketInfo<?>> REGISTERED = new ConcurrentHashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, SimpleChannel> TYPE_TO_CHANNEL = new ConcurrentHashMap<>();
    private static final Map<String, Integer> NEXT_ID = new ConcurrentHashMap<>();

    private static SimpleChannel channelOf(String modId) {
        return CHANNELS.computeIfAbsent(modId, id -> NetworkRegistry.newSimpleChannel(
                new ResourceLocation(id, CHANNEL_NAME),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        ));
    }

    private static List<ServerPlayer> levelPlayers(Entity e) {
        return (e.level() instanceof ServerLevel sl) ? sl.players() : Collections.emptyList();
    }

    private static List<ServerPlayer> levelPlayersIncluding(Entity e) {
        var list = levelPlayers(e);
        if (e instanceof ServerPlayer sp && !list.contains(sp)) {
            List<ServerPlayer> newList = new ArrayList<>(list);
            newList.add(sp);
            return newList;
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T extends INetworkPacket<T> & CustomPacketPayload> void registerAnnotated(Class<? extends INetworkPacket<?>> rawClass) {
        Class<T> clazz = (Class<T>) rawClass;
        var meta = clazz.getAnnotation(NetworkPacket.class);
        if (meta == null || !clazz.isRecord()) return;
        var type = NetworkPacketTypes.typeOf(clazz);
        var codec = NetworkSerialization.autoCodec(clazz);
        REGISTERED.put(type, new NetworkUtil.PacketInfo<>(type, codec));
        var side = meta.side();
        var channel = channelOf(type.id().getNamespace());
        TYPE_TO_CHANNEL.put(type, channel);
        int id = NEXT_ID.merge(type.id().getNamespace(), 1, Integer::sum) - 1;
        if (side == Side.SERVER || side == Side.BOTH) {
            channel.messageBuilder(clazz, id, NetworkDirection.PLAY_TO_SERVER)
                    .encoder((msg, buf) -> codec.encode(buf, (T) msg))
                    .decoder(codec::decode)
                    .consumerMainThread((msg, ctx) -> msg.handle(new ForgeNetworkContext(ctx.get())))
                    .add();
        }
        if (side == Side.CLIENT || side == Side.BOTH) {
            channel.messageBuilder(clazz, id, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder((msg, buf) -> codec.encode(buf, (T) msg))
                    .decoder(codec::decode)
                    .consumerMainThread((msg, ctx) -> msg.handle(new ForgeNetworkContext(ctx.get())))
                    .add();
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player) {
        if (player == null) return;
        executeSend(packet,
                ch -> ch.send(PacketDistributor.PLAYER.with(() -> player), packet),
                data -> NetworkUtil.sendChunkedPacket(data, getTypeId(packet), Collections.singletonList(player), getThreshold(packet))
        );
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet) {
        executeSend(packet,
                ch -> ch.sendToServer(packet),
                data -> NetworkUtil.sendChunkedPacketToServer(data, getTypeId(packet), getThreshold(packet))
        );
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAll(T packet) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        executeSend(packet,
                ch -> ch.send(PacketDistributor.ALL.noArg(), packet),
                data -> NetworkUtil.sendChunkedPacketToAll(data, getTypeId(packet), getThreshold(packet))
        );
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToWorld(T packet, ServerLevel level) {
        executeSend(packet,
                ch -> ch.send(PacketDistributor.DIMENSION.with(level::dimension), packet),
                data -> NetworkUtil.sendChunkedPacket(data, getTypeId(packet), level.players(), getThreshold(packet))
        );
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNear(T packet, ServerLevel level, Vec3 pos, double radius) {
        executeSend(packet,
                ch -> ch.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(pos.x, pos.y, pos.z, radius, level.dimension())), packet),
                data -> NetworkUtil.sendChunkedPacket(data, getTypeId(packet), level.players(), getThreshold(packet))
        );
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNearExcept(T packet, ServerLevel level, Vec3 pos, double radius, ServerPlayer excluded) {
        executeSend(packet,
                ch -> ch.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(pos.x, pos.y, pos.z, radius, level.dimension())), packet),
                data -> {
                    var players = level.players().stream().filter(p -> p != excluded).toList();
                    NetworkUtil.sendChunkedPacket(data, getTypeId(packet), players, getThreshold(packet));
                }
        );
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntity(T packet, Entity entity) {
        executeSend(packet,
                ch -> ch.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet),
                data -> NetworkUtil.sendChunkedPacket(data, getTypeId(packet), levelPlayers(entity), getThreshold(packet))
        );
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntityAndSelf(T packet, Entity entity) {
        executeSend(packet,
                ch -> ch.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet),
                data -> NetworkUtil.sendChunkedPacket(data, getTypeId(packet), levelPlayersIncluding(entity), getThreshold(packet))
        );
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingChunk(T packet, ServerLevel level, ChunkPos chunkPos) {
        executeSend(packet,
                ch -> {
                    var chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                    if (chunk != null) {
                        ch.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
                    }
                },
                data -> {
                    var players = level.getChunkSource().chunkMap.getPlayers(chunkPos, false);
                    NetworkUtil.sendChunkedPacket(data, getTypeId(packet), players, getThreshold(packet));
                }
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void executeSend(
            T packet,
            Consumer<SimpleChannel> normalSender,
            Consumer<byte[]> chunkedSender) {

        var type = NetworkPacketTypes.typeOf(packet.getClass());
        NetworkUtil.PacketInfo<T> info = (NetworkUtil.PacketInfo<T>) REGISTERED.get(type);
        if (info == null) return;

        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        NetworkUtil.sendWithChunking(
                packet,
                info,
                buf,
                threshold,
                () -> {
                    var ch = TYPE_TO_CHANNEL.get(type);
                    if (ch != null) normalSender.accept(ch);
                },
                chunkedSender
        );
    }

    private int getThreshold(INetworkPacket<?> packet) {
        return NetworkAutoRegistration.getChunkThreshold(packet.getClass());
    }

    @SuppressWarnings("unchecked")
    private ResourceLocation getTypeId(INetworkPacket<?> packet) {
        return NetworkPacketTypes.typeOf(packet.getClass()).id();
    }
}