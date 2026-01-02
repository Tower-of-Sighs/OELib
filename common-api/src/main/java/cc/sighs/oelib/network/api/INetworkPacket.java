package cc.sighs.oelib.network.api;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base interface for cross-platform network packets.
 * <p>
 * All custom packets should implement this interface and
 * {@link CustomPacketPayload}. The implementation is shared between
 * platforms, while platform specific managers handle registration and IO.
 * </p>
 *
 * @param <T> packet type
 */
public interface INetworkPacket<T extends INetworkPacket<T> & CustomPacketPayload> extends CustomPacketPayload {

    /**
     * Handles the packet on the receiving side.
     *
     * @param context platform independent network context
     */
    void handle(INetworkContext context);

    /**
     * Returns the packet type identifier.
     * <p>
     * The default implementation derives the type from {@link NetworkPacket}
     * metadata on the implementation class.
     * </p>
     *
     * @return packet type identifier
     */
    @SuppressWarnings("unchecked")
    @Override
    default CustomPacketPayload.Type<T> type() {
        Class<?> clazz = getClass();
        if (clazz.isAnnotationPresent(NetworkPacket.class)) {
            return NetworkPacketTypes.typeOf((Class<T>) clazz);
        }
        throw new IllegalStateException("Packet class " + clazz.getName() + " is missing @NetworkPacket");
    }

    /**
     * Sends this packet to a specific player.
     *
     * @param player target player
     */
    @SuppressWarnings("unchecked")
    default void sendTo(ServerPlayer player) {
        NetworkManager.sendToPlayer((T) this, player);
    }

    /**
     * Sends this packet to all players.
     */
    @SuppressWarnings("unchecked")
    default void sendToAll() {
        NetworkManager.sendToAll((T) this);
    }

    /**
     * Sends this packet to the logical server.
     */
    @SuppressWarnings("unchecked")
    default void sendToServer() {
        NetworkManager.sendToServer((T) this);
    }
}
