package cc.sighs.oelib.network.api;

import cc.sighs.oelib.network.spi.INetworkManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ServiceLoader;

/**
 * Static facade for platform specific network managers.
 * <p>
 * Provides platform independent helpers for sending packets. The actual
 * implementation is supplied by platform modules through the
 * {@link INetworkManager} service provider interface discovered via
 * {@link ServiceLoader}.
 * </p>
 */
public class NetworkManager {

    private static final INetworkManager IMPL;

    static {
        IMPL = ServiceLoader.load(INetworkManager.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No INetworkManager implementation found"));
    }

    public static void registerPacketScanPackage(String basePackage) {
        NetworkAutoRegistration.registerBasePackage(basePackage);
    }

    /**
     * Sends a packet to a specific player.
     *
     * @param packet packet instance
     * @param player target player
     * @param <T>    packet type
     */
    public static <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player) {
        IMPL.sendToPlayer(packet, player);
    }

    /**
     * Sends a packet to all players.
     *
     * @param packet packet instance
     * @param <T>    packet type
     */
    public static <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAll(T packet) {
        IMPL.sendToAll(packet);
    }

    /**
     * Sends a packet to the logical server.
     *
     * @param packet packet instance
     * @param <T>    packet type
     */
    public static <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet) {
        IMPL.sendToServer(packet);
    }

    /**
     * Sends a packet to all players in the given world.
     */
    public static <T extends INetworkPacket<T> & CustomPacketPayload> void sendToWorld(T packet, ServerLevel world) {
        IMPL.sendToWorld(packet, world);
    }

    /**
     * Sends a packet to players near a position in a world.
     */
    public static <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNear(T packet, ServerLevel world, Vec3 pos, double radius) {
        IMPL.sendToNear(packet, world, pos, radius);
    }

    /**
     * Sends a packet to players near a position in a world, excluding one player.
     */
    public static <T extends INetworkPacket<T> & CustomPacketPayload> void sendToNearExcept(T packet, ServerLevel world, Vec3 pos, double radius, ServerPlayer excluded) {
        IMPL.sendToNearExcept(packet, world, pos, radius, excluded);
    }

    /**
     * Sends a packet to all players tracking an entity.
     */
    public static <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntity(T packet, Entity entity) {
        IMPL.sendToTrackingEntity(packet, entity);
    }

    /**
     * Sends a packet to all players tracking an entity and the entity itself if a player.
     */
    public static <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingEntityAndSelf(T packet, Entity entity) {
        IMPL.sendToTrackingEntityAndSelf(packet, entity);
    }

    /**
     * Sends a packet to all players tracking the given chunk.
     */
    public static <T extends INetworkPacket<T> & CustomPacketPayload> void sendToTrackingChunk(T packet, ServerLevel level, ChunkPos chunkPos) {
        IMPL.sendToTrackingChunk(packet, level, chunkPos);
    }
}
