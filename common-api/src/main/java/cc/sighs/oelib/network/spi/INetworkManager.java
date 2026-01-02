package cc.sighs.oelib.network.spi;

import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Service Provider Interface for platform network managers.
 * <p>
 * Implementations live in platform specific modules and are responsible for
 * sending packets through the underlying networking system. The common
 * {@link NetworkManager} facade delegates to the active implementation.
 * </p>
 */
public interface INetworkManager {

    /**
     * Sends a packet to a specific player.
     *
     * @param packet packet instance
     * @param player target player
     * @param <T>    packet type
     */
    <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player);

    /**
     * Sends a packet to all players.
     *
     * @param packet packet instance
     * @param <T>    packet type
     */
    <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAll(T packet);

    /**
     * Sends a packet to the logical server.
     *
     * @param packet packet instance
     * @param <T>    packet type
     */
    <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet);
}
