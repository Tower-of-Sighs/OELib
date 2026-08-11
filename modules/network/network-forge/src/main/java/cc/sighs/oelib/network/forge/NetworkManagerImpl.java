package cc.sighs.oelib.network.forge;

import cc.sighs.oelib.network.OELibNetwork;
import cc.sighs.oelib.network.api.*;
import cc.sighs.oelib.network.serialization.NetworkSerialization;
import cc.sighs.oelib.network.spi.INetworkManager;
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

public class NetworkManagerImpl implements INetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    private static final String SERVERBOUND_CHANNEL_NAME = "oelib_c2s";
    private static final String CLIENTBOUND_CHANNEL_NAME = "oelib_s2c";
    private static final Map<String, SimpleChannel> SERVERBOUND_CHANNELS = new ConcurrentHashMap<>();
    private static final Map<String, SimpleChannel> CLIENTBOUND_CHANNELS = new ConcurrentHashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, SimpleChannel> TYPE_TO_SERVERBOUND_CHANNEL = new ConcurrentHashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, SimpleChannel> TYPE_TO_CLIENTBOUND_CHANNEL = new ConcurrentHashMap<>();
    private static final Map<String, Integer> NEXT_SERVERBOUND_ID = new ConcurrentHashMap<>();
    private static final Map<String, Integer> NEXT_CLIENTBOUND_ID = new ConcurrentHashMap<>();
    private static volatile boolean registered = false;

    private static SimpleChannel serverboundChannelOf(String modId) {
        return SERVERBOUND_CHANNELS.computeIfAbsent(modId, id -> NetworkRegistry.newSimpleChannel(
                new ResourceLocation(id, SERVERBOUND_CHANNEL_NAME),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        ));
    }

    private static SimpleChannel clientboundChannelOf(String modId) {
        return CLIENTBOUND_CHANNELS.computeIfAbsent(modId, id -> NetworkRegistry.newSimpleChannel(
                new ResourceLocation(id, CLIENTBOUND_CHANNEL_NAME),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        ));
    }

    /**
     * Registers every annotated packet into the Forge {@link SimpleChannel}s.
     * Called once from the mod constructor after the global scan index is ready.
     */
    public static synchronized void registerAll() {
        if (registered) {
            return;
        }
        registered = true;
        NetworkManagerImpl impl = new NetworkManagerImpl();
        for (Class<? extends INetworkPacket<?>> packetClass : NetworkAutoRegistration.findAllAnnotatedPackets()) {
            impl.registerAnnotated(packetClass);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerAnnotated(Class<? extends INetworkPacket<?>> rawClass) {
        Class<T> clazz = (Class<T>) rawClass;
        var meta = clazz.getAnnotation(NetworkPacket.class);
        if (meta == null || !clazz.isRecord()) return;
        var type = NetworkPacketTypes.typeOf(clazz);
        var side = meta.side();
        var codec = NetworkSerialization.autoCodec(clazz);
        /*
         * Forge's IndexedMessageCodec uses a single map for ID/Direction mapping.
         * Registering the same class for BOTH sides on one channel causes the
         * second registration (usually S2C) to overwrite the first (C2S).
         * This leads to a 'PLAY_TO_SERVER' packet being validated against a
         * 'PLAY_TO_CLIENT' expectation on the server, triggering a disconnect.
         *
         * We separate traffic into two physical channels: 'oelib_c2s' and 'oelib_s2c'.
         */
        if (side == Side.SERVER || side == Side.BOTH) {
            var channel = serverboundChannelOf(type.id().getNamespace());
            TYPE_TO_SERVERBOUND_CHANNEL.put(type, channel);
            int id = NEXT_SERVERBOUND_ID.merge(type.id().getNamespace(), 1, Integer::sum) - 1;
            channel.messageBuilder(clazz, id, NetworkDirection.PLAY_TO_SERVER)
                    .encoder((msg, buf) -> codec.encode(buf, msg))
                    .decoder(codec::decode)
                    .consumerMainThread((msg, ctx) -> msg.handle(new ForgeNetworkContext(ctx.get())))
                    .add();
        }
        if (side == Side.CLIENT || side == Side.BOTH) {
            var channel = clientboundChannelOf(type.id().getNamespace());
            TYPE_TO_CLIENTBOUND_CHANNEL.put(type, channel);
            int id = NEXT_CLIENTBOUND_ID.merge(type.id().getNamespace(), 1, Integer::sum) - 1;
            channel.messageBuilder(clazz, id, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder((msg, buf) -> codec.encode(buf, msg))
                    .decoder(codec::decode)
                    .consumerMainThread((msg, ctx) -> msg.handle(new ForgeNetworkContext(ctx.get())))
                    .add();
        }
        OELibNetwork.LOGGER.info("Forge registration for {}: Side={}, TypeID={}", clazz.getSimpleName(), side, type.id());
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player) {
        if (player == null) return;
        var ch = TYPE_TO_CLIENTBOUND_CHANNEL.get(NetworkPacketTypes.typeOf(packet.getClass()));
        if (ch != null) {
            ch.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet) {
        var ch = TYPE_TO_SERVERBOUND_CHANNEL.get(NetworkPacketTypes.typeOf(packet.getClass()));
        if (ch != null) {
            ch.sendToServer(packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAll(T packet) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var ch = TYPE_TO_CLIENTBOUND_CHANNEL.get(NetworkPacketTypes.typeOf(packet.getClass()));
        if (ch != null) {
            ch.send(PacketDistributor.ALL.noArg(), packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToWorld(T packet, ServerLevel level) {
        var ch = TYPE_TO_CLIENTBOUND_CHANNEL.get(NetworkPacketTypes.typeOf(packet.getClass()));
        if (ch != null) {
            ch.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNear(T packet, ServerLevel level, Vec3 pos, double radius) {
        var ch = TYPE_TO_CLIENTBOUND_CHANNEL.get(NetworkPacketTypes.typeOf(packet.getClass()));
        if (ch != null) {
            ch.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(pos.x, pos.y, pos.z, radius, level.dimension())), packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNearExcept(T packet, ServerLevel level, Vec3 pos, double radius, ServerPlayer excluded) {
        var ch = TYPE_TO_CLIENTBOUND_CHANNEL.get(NetworkPacketTypes.typeOf(packet.getClass()));
        if (ch != null) {
            ch.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(pos.x, pos.y, pos.z, radius, level.dimension())), packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntity(T packet, Entity entity) {
        var ch = TYPE_TO_CLIENTBOUND_CHANNEL.get(NetworkPacketTypes.typeOf(packet.getClass()));
        if (ch != null) {
            ch.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntityAndSelf(T packet, Entity entity) {
        var ch = TYPE_TO_CLIENTBOUND_CHANNEL.get(NetworkPacketTypes.typeOf(packet.getClass()));
        if (ch != null) {
            ch.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingChunk(T packet, ServerLevel level, ChunkPos chunkPos) {
        var ch = TYPE_TO_CLIENTBOUND_CHANNEL.get(NetworkPacketTypes.typeOf(packet.getClass()));
        if (ch != null) {
            var chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
            if (chunk != null) {
                ch.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
            }
        }
    }
}
