package cc.sighs.oelib.fabric.network;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.fabric.data.DataManager;
import cc.sighs.oelib.network.api.*;
import cc.sighs.oelib.network.serialization.NetworkSerialization;
import cc.sighs.oelib.network.spi.INetworkManager;
import cc.sighs.oelib.network.util.NetworkUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManagerImpl implements INetworkManager {

    private static final Map<CustomPacketPayload.Type<?>, NetworkUtil.PacketInfo<?>> registeredPackets = new ConcurrentHashMap<>();

    public static void initialize() {
        processRegistration(RegistrationPhase.COMMON);
    }

    public static void initializeClient() {
        processRegistration(RegistrationPhase.CLIENT);
    }

    private static void processRegistration(RegistrationPhase phase) {
        NetworkManagerImpl impl = new NetworkManagerImpl();
        for (Class<? extends INetworkPacket<?>> packetClass : NetworkAutoRegistration.findAllAnnotatedPackets()) {
            impl.registerAnnotated(packetClass, phase);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerAnnotated(Class<? extends INetworkPacket<?>> rawClass, RegistrationPhase phase) {
        Class<T> clazz = (Class<T>) rawClass;
        var meta = clazz.getAnnotation(NetworkPacket.class);
        if (meta == null || !clazz.isRecord()) return;

        var type = NetworkPacketTypes.typeOf(clazz);
        var side = meta.side();

        if (phase == RegistrationPhase.COMMON) {
            var codec = NetworkSerialization.autoCodec(clazz);
            registeredPackets.put(type, new NetworkUtil.PacketInfo<>(type, codec));

            if (side == Side.CLIENT || side == Side.BOTH) {
                PayloadTypeRegistry.clientboundPlay().register(type, codec);
            }

            if (side == Side.SERVER || side == Side.BOTH) {
                PayloadTypeRegistry.serverboundPlay().register(type, codec);
            }

            if (side == Side.SERVER || side == Side.BOTH) {
                ServerPlayNetworking.registerGlobalReceiver(type, (packet, context) ->
                        context.server().execute(() -> packet.handle(new FabricServerNetworkContext(context))));
            }

            OELib.LOGGER.info("Common registration for {}: Side={}, TypeID={}", clazz.getSimpleName(), side, type.id());

        } else if (phase == RegistrationPhase.CLIENT) {
            if (side == Side.CLIENT || side == Side.BOTH) {
                ClientPlayNetworking.registerGlobalReceiver(type, (packet, context) ->
                        context.client().execute(() -> packet.handle(new FabricClientNetworkContext(context))));

                OELib.LOGGER.info("Client receiver registered for: {}", clazz.getSimpleName());
            }
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet) {
        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        if (threshold <= 0) {
            ClientPlayNetworking.send(packet);
            return;
        }

        @SuppressWarnings("unchecked")
        NetworkUtil.PacketInfo<T> info = (NetworkUtil.PacketInfo<T>) registeredPackets.get(packet.type());
        if (info == null) {
            ClientPlayNetworking.send(packet);
            return;
        }

        var buf = NetworkUtil.createClientBuffer();

        NetworkUtil.sendWithChunking(
                packet,
                info,
                buf,
                threshold,
                () -> ClientPlayNetworking.send(packet),
                data -> NetworkUtil.sendChunkedPacketToServer(data, packet.type().id(), threshold)
        );
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player) {
        if (player == null) return;

        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        if (threshold > 0) {
            sendWithChunking(packet, Collections.singletonList(player), threshold);
        } else {
            ServerPlayNetworking.send(player, packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAll(T packet) {
        var server = DataManager.getCurrentServer();
        if (server == null) return;

        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        var players = PlayerLookup.all(server);
        if (players.isEmpty()) return;

        if (threshold > 0) {
            @SuppressWarnings("unchecked")
            NetworkUtil.PacketInfo<T> info = (NetworkUtil.PacketInfo<T>) registeredPackets.get(packet.type());
            if (info == null) return;

            var buf = NetworkUtil.createBufferFromFirstPlayer(players);

            try {
                byte[] data = NetworkUtil.encodePacket(packet, info, buf);
                NetworkUtil.sendChunkedPacketToAll(data, packet.type().id(), threshold);
            } finally {
                buf.release();
            }
        } else {
            for (ServerPlayer player : players) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToWorld(T packet, ServerLevel level) {
        var players = PlayerLookup.level(level);
        if (players.isEmpty()) return;
        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        sendWithChunking(packet, players, threshold);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNear(T packet, ServerLevel level, Vec3 pos, double radius) {
        var players = PlayerLookup.around(level, pos, radius);
        if (players.isEmpty()) return;
        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        sendWithChunking(packet, players, threshold);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNearExcept(T packet, ServerLevel level, Vec3 pos, double radius, ServerPlayer excluded) {
        var players = PlayerLookup.around(level, pos, radius).stream().filter(p -> p != excluded).toList();
        if (players.isEmpty()) return;
        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        sendWithChunking(packet, players, threshold);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntity(T packet, Entity entity) {
        var players = PlayerLookup.tracking(entity);
        if (players.isEmpty()) return;
        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        sendWithChunking(packet, players, threshold);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntityAndSelf(T packet, Entity entity) {
        var list = new ArrayList<>(PlayerLookup.tracking(entity));
        if (entity instanceof ServerPlayer sp && !list.contains(sp)) {
            list.add(sp);
        }
        if (list.isEmpty()) return;
        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        sendWithChunking(packet, list, threshold);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingChunk(T packet, ServerLevel level, ChunkPos chunkPos) {
        var players = PlayerLookup.tracking(level, chunkPos);
        if (players.isEmpty()) return;
        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        sendWithChunking(packet, players, threshold);
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void sendWithChunking(
            T packet,
            Collection<ServerPlayer> players,
            int threshold) {

        if (players.isEmpty()) return;

        NetworkUtil.PacketInfo<T> info = (NetworkUtil.PacketInfo<T>) registeredPackets.get(packet.type());
        if (info == null) return;

        var buf = NetworkUtil.createBufferFromFirstPlayer(players);

        NetworkUtil.sendWithChunking(
                packet,
                info,
                buf,
                threshold,
                () -> players.forEach(p -> ServerPlayNetworking.send(p, packet)),
                data -> NetworkUtil.sendChunkedPacket(data, packet.type().id(), players, threshold)
        );
    }

    private enum RegistrationPhase {
        COMMON, CLIENT
    }
}
