package cc.sighs.oelib.network.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * 网络管理器接口。
 * <p>
 * 定义了网络管理器的基本功能，由各平台实现。
 * </p>
 */
public interface INetworkManager {

    /**
     * 发送网络包到指定玩家。
     *
     * @param packet 网络包
     * @param player 目标玩家
     * @param <T>    网络包类型
     */
    <T extends INetworkPacket<T>> void sendToPlayer(T packet, ServerPlayer player);

    /**
     * 发送网络包到所有玩家。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    <T extends INetworkPacket<T>> void sendToAll(T packet);

    /**
     * 发送网络包到服务器。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    <T extends INetworkPacket<T>> void sendToServer(T packet);

    /**
     * 发送网络包到指定玩家（支持自动分片）。
     *
     * @param packet 网络包
     * @param player 目标玩家
     * @param <T>    网络包类型
     */
    <T extends INetworkPacket<T>> void sendToPlayerWithChunking(T packet, ServerPlayer player);

    /**
     * 发送网络包到所有玩家（支持自动分片）。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    <T extends INetworkPacket<T>> void sendToAllWithChunking(T packet);

    /**
     * 注册网络包。
     *
     * @param packetClasses 要注册的网络包类
     */
    void registerPackets(Class<? extends INetworkPacket<?>>... packetClasses);
}