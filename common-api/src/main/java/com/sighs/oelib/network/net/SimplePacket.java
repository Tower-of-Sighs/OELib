package com.sighs.oelib.network.net;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 简单网络包接口。
 * <p>
 * 提供一些常用的便利方法，让modder更容易创建网络包。
 * 这是一个跨平台的接口，可以在common模块中使用。
 * </p>
 *
 * @param <T> 网络包类型
 */
public interface SimplePacket<T extends SimplePacket<T> & CustomPacketPayload> extends INetworkPacket<T> {

    /**
     * 获取模组ID。
     * <p>
     * 实现类必须实现此方法以返回对应的模组ID。
     * </p>
     *
     * @return 模组ID
     */
    String getModId();

    @Override
    default CustomPacketPayload.Type<T> type() {
        String className = this.getClass().getSimpleName().toLowerCase();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(getModId(), className);
        return new CustomPacketPayload.Type<>(id);
    }

    /**
     * 检查是否在客户端。
     *
     * @param context 网络上下文
     * @return 如果在客户端返回 true
     */
    default boolean isClientSide(INetworkContext context) {
        return context.isClientSide();
    }

    /**
     * 检查是否在服务端。
     *
     * @param context 网络上下文
     * @return 如果在服务端返回 true
     */
    default boolean isServerSide(INetworkContext context) {
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
    default ServerPlayer getSender(INetworkContext context) {
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
    default Minecraft getClient(INetworkContext context) {
        return context.client();
    }

    /**
     * 在客户端处理网络包。
     * <p>
     * 默认实现为空，实现类可以重写此方法。
     * </p>
     *
     * @param context 网络上下文
     */
    default void handleClient(INetworkContext context) {
        // 默认空实现
    }

    /**
     * 在服务端处理网络包。
     * <p>
     * 默认实现为空，实现类可以重写此方法。
     * </p>
     *
     * @param context 网络上下文
     */
    default void handleServer(INetworkContext context) {
        // 默认空实现
    }

    @Override
    default void handle(INetworkContext context) {
        if (isClientSide(context)) {
            handleClient(context);
        } else {
            handleServer(context);
        }
    }
}