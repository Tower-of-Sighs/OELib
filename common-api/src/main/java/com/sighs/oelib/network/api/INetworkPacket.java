package com.sighs.oelib.network.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * 通用网络包接口。
 * <p>
 * 所有自定义网络包都应该实现此接口。
 * 这是一个跨平台的接口，可以在common模块中使用。
 * </p>
 *
 * @param <T> 网络包类型
 */
public interface INetworkPacket<T extends INetworkPacket<T>> {

    /**
     * 将数据包编码到缓冲区。
     *
     * @param buf 缓冲区
     */
    void encode(FriendlyByteBuf buf);

    /**
     * 处理网络包。
     * <p>
     * 此方法在接收端被调用。
     * </p>
     *
     * @param context 网络上下文（平台无关）
     */
    void handle(INetworkContext context);

    /**
     * 发送到指定玩家。
     *
     * @param player 目标玩家
     */
    @SuppressWarnings("unchecked")
    default void sendTo(ServerPlayer player) {
        NetworkManager.sendToPlayer((T) this, player);
    }

    /**
     * 发送到所有玩家。
     */
    @SuppressWarnings("unchecked")
    default void sendToAll() {
        NetworkManager.sendToAll((T) this);
    }

    /**
     * 发送到服务器。
     */
    @SuppressWarnings("unchecked")
    default void sendToServer() {
        NetworkManager.sendToServer((T) this);
    }

    /**
     * 发送到指定玩家（支持自动分片）。
     *
     * @param player 目标玩家
     */
    @SuppressWarnings("unchecked")
    default void sendToWithChunking(ServerPlayer player) {
        NetworkManager.sendToPlayerWithChunking((T) this, player);
    }

    /**
     * 发送到所有玩家（支持自动分片）。
     */
    @SuppressWarnings("unchecked")
    default void sendToAllWithChunking() {
        NetworkManager.sendToAllWithChunking((T) this);
    }
}