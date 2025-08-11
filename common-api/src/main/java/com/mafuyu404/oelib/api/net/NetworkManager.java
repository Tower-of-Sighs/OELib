package com.mafuyu404.oelib.api.net;

import net.minecraft.server.level.ServerPlayer;

/**
 * 通用网络管理器接口。
 * <p>
 * 提供平台无关的网络包发送功能。
 * 具体实现由各平台的NetworkManager提供。
 * </p>
 */
public class NetworkManager {
    
    private static INetworkManager instance;
    
    /**
     * 设置网络管理器实例。
     * <p>
     * 此方法由各平台的NetworkManager在初始化时调用。
     * </p>
     *
     * @param manager 网络管理器实例
     */
    public static void setInstance(INetworkManager manager) {
        instance = manager;
    }
    
    /**
     * 发送网络包到指定玩家。
     *
     * @param packet 网络包
     * @param player 目标玩家
     * @param <T>    网络包类型
     */
    public static <T extends INetworkPacket<T>> void sendToPlayer(T packet, ServerPlayer player) {
        if (instance != null) {
            instance.sendToPlayer(packet, player);
        }
    }
    
    /**
     * 发送网络包到所有玩家。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    public static <T extends INetworkPacket<T>> void sendToAll(T packet) {
        if (instance != null) {
            instance.sendToAll(packet);
        }
    }
    
    /**
     * 发送网络包到服务器。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    public static <T extends INetworkPacket<T>> void sendToServer(T packet) {
        if (instance != null) {
            instance.sendToServer(packet);
        }
    }
    
    /**
     * 发送网络包到指定玩家（支持自动分片）。
     *
     * @param packet 网络包
     * @param player 目标玩家
     * @param <T>    网络包类型
     */
    public static <T extends INetworkPacket<T>> void sendToPlayerWithChunking(T packet, ServerPlayer player) {
        if (instance != null) {
            instance.sendToPlayerWithChunking(packet, player);
        }
    }
    
    /**
     * 发送网络包到所有玩家（支持自动分片）。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    public static <T extends INetworkPacket<T>> void sendToAllWithChunking(T packet) {
        if (instance != null) {
            instance.sendToAllWithChunking(packet);
        }
    }
    
    /**
     * 注册网络包。
     *
     * @param packetClasses 要注册的网络包类
     */
    @SafeVarargs
    public static void registerPackets(Class<? extends INetworkPacket<?>>... packetClasses) {
        if (instance != null) {
            instance.registerPackets(packetClasses);
        }
    }
}