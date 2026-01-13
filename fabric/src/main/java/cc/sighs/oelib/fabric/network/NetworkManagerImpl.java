package cc.sighs.oelib.fabric.network;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.fabric.data.DataManager;
import cc.sighs.oelib.network.api.*;
import cc.sighs.oelib.network.serialization.NetworkSerialization;
import cc.sighs.oelib.network.spi.INetworkManager;
import cc.sighs.oelib.network.util.NetworkUtil;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManagerImpl implements INetworkManager {

    private static final Map<CustomPacketPayload.Type<?>, PacketInfo<?>> registeredPackets = new ConcurrentHashMap<>();

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
            registeredPackets.put(type, new PacketInfo<>(type, codec));

            if (side == Side.CLIENT || side == Side.BOTH) {
                PayloadTypeRegistry.playS2C().register(type, codec);
            }

            if (side == Side.SERVER || side == Side.BOTH) {
                PayloadTypeRegistry.playC2S().register(type, codec);
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
        ClientPlayNetworking.send(packet);
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
            sendWithChunking(packet, players, threshold);
        } else {
            for (ServerPlayer player : players) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void sendWithChunking(T packet, Collection<ServerPlayer> players, int threshold) {
        PacketInfo<T> info = (PacketInfo<T>) registeredPackets.get(packet.type());
        if (info == null) return;

        var firstPlayer = players.iterator().next();
        var registries = firstPlayer.registryAccess();

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);

        info.codec().encode(buf, packet);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();

        if (data.length <= threshold) {
            players.forEach(p -> ServerPlayNetworking.send(p, packet));
        } else {
            NetworkUtil.sendChunkedPacket(data, packet.getClass().getName(), players, threshold);
        }
    }

    private enum RegistrationPhase {
        COMMON, CLIENT
    }

    private record PacketInfo<T extends INetworkPacket<T> & CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {}
}