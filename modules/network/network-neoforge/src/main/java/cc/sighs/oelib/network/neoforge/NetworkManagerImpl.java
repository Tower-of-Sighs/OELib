package cc.sighs.oelib.network.neoforge;

import cc.sighs.oelib.network.OELibNetwork;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkAutoRegistration;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacketTypes;
import cc.sighs.oelib.network.serialization.NetworkSerialization;
import cc.sighs.oelib.network.spi.INetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = OELibNetwork.MOD_ID)
public class NetworkManagerImpl implements INetworkManager {

    private static final String PROTOCOL_VERSION = "1";
    private static final Set<CustomPacketPayload.Type<?>> registeredPackets = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar(OELibNetwork.MOD_ID).versioned(PROTOCOL_VERSION);
        NetworkManagerImpl impl = new NetworkManagerImpl();

        for (Class<? extends INetworkPacket<?>> packetClass : NetworkAutoRegistration.findAllAnnotatedPackets()) {
            impl.registerAnnotated(packetClass, registrar);
        }
    }

    public static int getRegisteredPacketCount() {
        return registeredPackets.size();
    }

    public static Set<CustomPacketPayload.Type<?>> getRegisteredPacketTypes() {
        return Set.copyOf(registeredPackets);
    }

    private <T extends INetworkPacket<T>> void handle(T packet, IPayloadContext context) {
        context.enqueueWork(() -> packet.handle(new NeoForgeNetworkContext(context)));
    }

    @SuppressWarnings("unchecked")
    private <T extends INetworkPacket<T> & CustomPacketPayload> void registerAnnotated(Class<? extends INetworkPacket<?>> rawClass, PayloadRegistrar registrar) {
        Class<T> clazz = (Class<T>) rawClass;
        var meta = clazz.getAnnotation(NetworkPacket.class);
        if (meta == null || !clazz.isRecord()) return;

        CustomPacketPayload.Type<T> type = NetworkPacketTypes.typeOf(clazz);
        StreamCodec<RegistryFriendlyByteBuf, T> codec = NetworkSerialization.autoCodec(clazz);
        registeredPackets.add(type);

        var side = meta.side();
        OELibNetwork.LOGGER.debug("Registering packet: {} | Side: {} | Type ID: {}", clazz.getSimpleName(), side, type.id());
        switch (side) {
            case CLIENT -> registrar.playToClient(type, codec, this::handle);
            case SERVER -> registrar.playToServer(type, codec, this::handle);
            case BOTH -> registrar.playBidirectional(type, codec, this::handle);
        }
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAll(T packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToWorld(T packet, ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, packet);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNear(T packet, ServerLevel level, Vec3 pos, double radius) {
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, radius, packet);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNearExcept(T packet, ServerLevel level, Vec3 pos, double radius, ServerPlayer excluded) {
        PacketDistributor.sendToPlayersNear(level, excluded, pos.x, pos.y, pos.z, radius, packet);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntity(T packet, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntityAndSelf(T packet, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet);
    }

    @Override
    public <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingChunk(T packet, ServerLevel level, ChunkPos chunkPos) {
        PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, packet);
    }
}
