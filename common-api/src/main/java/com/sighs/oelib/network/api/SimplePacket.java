package com.sighs.oelib.network.api;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

/**
 * 简单网络包基类。
 * <p>
 * 提供一些常用的便利方法，让modder更容易创建网络包。
 * 这是一个跨平台的基类，可以在common模块中使用。
 * </p>
 *
 * @param <T> 网络包类型
 */
public abstract class SimplePacket<T extends SimplePacket<T>> implements INetworkPacket<T> {

    /**
     * 检查是否在客户端。
     *
     * @param context 网络上下文
     * @return 如果在客户端返回 true
     */
    protected boolean isClientSide(INetworkContext context) {
        return context.isClientSide();
    }

    /**
     * 检查是否在服务端。
     *
     * @param context 网络上下文
     * @return 如果在服务端返回 true
     */
    protected boolean isServerSide(INetworkContext context) {
        return context.isServerSide();
    }

    /**
     * 获取发送者玩家。
     * <p>
     * 仅在服务端有效。
     * </p>
     *
     * @param context 网络上下文
     * @return 发送者玩家，如果在客户端或无法获取则返回 null
     */
    protected ServerPlayer getSender(INetworkContext context) {
        return context.sender();
    }

    /**
     * 获取客户端实例。
     * <p>
     * 仅在客户端有效。
     * </p>
     *
     * @param context 网络上下文
     * @return 客户端实例，如果在服务端或无法获取则返回 null
     */
    protected Minecraft getClient(INetworkContext context) {
        return context.client();
    }

    /**
     * 在客户端处理网络包。
     * <p>
     * 默认实现为空，子类可以重写此方法。
     * </p>
     *
     * @param context 网络上下文
     */
    protected void handleClient(INetworkContext context) {
        // 默认空实现
    }

    /**
     * 在服务端处理网络包。
     * <p>
     * 默认实现为空，子类可以重写此方法。
     * </p>
     *
     * @param context 网络上下文
     */
    protected void handleServer(INetworkContext context) {
        // 默认空实现
    }

    @Override
    public final void handle(INetworkContext context) {
        if (isClientSide(context)) {
            handleClient(context);
        } else {
            handleServer(context);
        }
    }
}