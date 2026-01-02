package cc.sighs.oelib.neoforge.network;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.neoforge.data.DataManager;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkAutoRegistration;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacketTypes;
import cc.sighs.oelib.network.serialization.NetworkSerialization;
import cc.sighs.oelib.network.spi.INetworkManager;
import cc.sighs.oelib.network.util.NetworkUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = OELib.MODID)
public class NetworkManagerImpl implements INetworkManager {

    private static final String PROTOCOL_VERSION = "1";
    private static final Map<CustomPacketPayload.Type<?>, PacketInfo<?>> registeredPackets = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(OELib.MODID).versioned(PROTOCOL_VERSION);
        NetworkManagerImpl impl = new NetworkManagerImpl();

        for (Class<? extends INetworkPacket<?>> packetClass : NetworkAutoRegistration.findAllAnnotatedPackets()) {
            impl.registerAnnotated(packetClass, registrar);
        }
    }

    public static int getRegisteredPacketCount() {
        return registeredPackets.size();
    }

    public static Set<CustomPacketPayload.Type<?>> getRegisteredPacketTypes() {
        return new HashSet<>(registeredPackets.keySet());
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerAnnotated(Class<? extends INetworkPacket<?>> rawClass, PayloadRegistrar registrar) {
        Class<T> clazz = (Class<T>) rawClass;
        NetworkPacket meta = clazz.getAnnotation(NetworkPacket.class);
        if (meta == null || !clazz.isRecord()) return;

        CustomPacketPayload.Type<T> type = NetworkPacketTypes.typeOf(clazz);
        StreamCodec<RegistryFriendlyByteBuf, T> codec = NetworkSerialization.autoCodec(clazz);
        registeredPackets.put(type, new PacketInfo<>(type, codec));

        switch (meta.side()) {
            case CLIENT -> registrar.playToClient(type, codec, this::handle);
            case SERVER -> registrar.playToServer(type, codec, this::handle);
            case BOTH -> registrar.playBidirectional(type, codec, this::handle);
        }
    }

    private <T extends INetworkPacket<T>> void handle(T packet, IPayloadContext context) {
        context.enqueueWork(() -> packet.handle(new NeoForgeNetworkContext(context)));
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player) {
        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        if (threshold > 0) {
            sendWithChunking(packet, Collections.singletonList(player), threshold);
        } else {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAll(T packet) {
        int threshold = NetworkAutoRegistration.getChunkThreshold(packet.getClass());
        MinecraftServer server = DataManager.getCurrentServer();
        if (threshold > 0 && server != null) {
            sendWithChunking(packet, server.getPlayerList().getPlayers(), threshold);
        } else {
            PacketDistributor.sendToAllPlayers(packet);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void sendWithChunking(T packet, Collection<ServerPlayer> players, int threshold) {
        PacketInfo<T> info = (PacketInfo<T>) registeredPackets.get(packet.type());
        if (info == null || players.isEmpty()) return;

        var firstPlayer = players.iterator().next();
        var registries = firstPlayer.registryAccess();

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                registries,
                ConnectionType.NEOFORGE
        );

        try {
            info.codec().encode(buf, packet);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);

            if (data.length <= threshold) {
                players.forEach(p -> PacketDistributor.sendToPlayer(p, packet));
                OELib.LOGGER.debug("Sent packet {} without chunking ({} bytes)", packet.type().id(), data.length);
            } else {
                NetworkUtil.sendChunkedPacket(data, packet.getClass().getName(), players, threshold);
            }
        } finally {
            buf.release();
        }
    }

    public record PacketInfo<T extends INetworkPacket<T> & CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
    }
}