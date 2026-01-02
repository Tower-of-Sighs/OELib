package cc.sighs.oelib.network.api;

import cc.sighs.oelib.network.spi.INetworkManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

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
}
