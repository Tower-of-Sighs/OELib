package com.mafuyu404.oelib.api.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
    <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayer(T packet, ServerPlayer player);

    /**
     * 发送网络包到所有玩家。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAll(T packet);

    /**
     * 发送网络包到服务器。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    <T extends INetworkPacket<T> & CustomPacketPayload> void sendToServer(T packet);

    /**
     * 发送网络包到指定玩家（支持自动分片）。
     *
     * @param packet 网络包
     * @param player 目标玩家
     * @param <T>    网络包类型
     */
    <T extends INetworkPacket<T> & CustomPacketPayload> void sendToPlayerWithChunking(T packet, ServerPlayer player);

    /**
     * 发送网络包到所有玩家（支持自动分片）。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    <T extends INetworkPacket<T> & CustomPacketPayload> void sendToAllWithChunking(T packet);

    /**
     * 注册网络包。
     *
     * @param packetClass 网络包类
     * @param codec       编解码器
     * @param <T>         网络包类型
     */
    <T extends INetworkPacket<T> & CustomPacketPayload> void registerPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    );

    /**
     * 批量注册网络包。
     *
     * @param packets 网络包注册信息
     */
    void registerPackets(PacketRegistration<?>... packets);

    /**
     * 注册客户端网络包。
     *
     * @param packetClass 网络包类
     * @param codec       编解码器
     * @param <T>         网络包类型
     */
    <T extends INetworkPacket<T> & CustomPacketPayload> void registerClientPacket(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    );

    /**
     * 批量注册客户端网络包。
     *
     * @param packets 网络包注册信息
     */
    void registerClientPackets(PacketRegistration<?>... packets);

    /**
     * 网络包注册信息。
     *
     * @param packetClass 网络包类
     * @param codec       编解码器
     * @param <T>         网络包类型
     */
    record PacketRegistration<T extends INetworkPacket<T> & CustomPacketPayload>(
            Class<T> packetClass,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {}
}