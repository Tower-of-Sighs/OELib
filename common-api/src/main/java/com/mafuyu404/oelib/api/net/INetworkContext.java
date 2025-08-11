package com.mafuyu404.oelib.api.net;

import net.minecraft.server.level.ServerPlayer;

/**
 * 网络上下文接口。
 * <p>
 * 提供平台无关的网络上下文信息。
 * </p>
 */
public interface INetworkContext {
    
    /**
     * 检查是否在客户端。
     *
     * @return 如果在客户端返回 true
     */
    boolean isClientSide();
    
    /**
     * 检查是否在服务端。
     *
     * @return 如果在服务端返回 true
     */
    boolean isServerSide();
    
    /**
     * 获取发送者玩家。
     * <p>
     * 仅在服务端有效。
     * </p>
     *
     * @return 发送者玩家，如果在客户端或无法获取则返回 null
     */
    ServerPlayer sender();
}