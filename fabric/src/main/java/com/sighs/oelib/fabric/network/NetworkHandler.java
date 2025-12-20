package com.sighs.oelib.fabric.network;

import com.sighs.oelib.OELib;
import com.sighs.oelib.fabric.data.DataManager;
import com.sighs.oelib.network.api.INetworkPacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric网络处理器。
 */
public class NetworkHandler {

    /**
     * 发送网络包到指定玩家。
     *
     * @param player 目标玩家
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    public static <T extends INetworkPacket<T>> void sendTo(ServerPlayer player, T packet) {
        ResourceLocation id = new ResourceLocation(OELib.MODID, packet.getClass().getSimpleName().toLowerCase());
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        ServerPlayNetworking.send(player, id, buf);
    }

    /**
     * 发送网络包到所有玩家。
     *
     * @param packet 网络包
     * @param <T>    网络包类型
     */
    public static <T extends INetworkPacket<T>> void sendToAll(T packet) {
        ResourceLocation id = new ResourceLocation(OELib.MODID, packet.getClass().getSimpleName().toLowerCase());
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);

        sendRawToAll(id, buf);
    }

    /**
     * 发送原始数据包到指定玩家（用于内置数据包）。
     *
     * @param player 目标玩家
     * @param id     数据包ID
     * @param buf    数据缓冲区
     */
    public static void sendRaw(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, id, buf);
    }

    /**
     * 发送原始数据包到所有玩家（用于内置数据包）。
     *
     * @param id  数据包ID
     * @param buf 数据缓冲区
     */
    public static void sendRawToAll(ResourceLocation id, FriendlyByteBuf buf) {
        MinecraftServer server = DataManager.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                ServerPlayNetworking.send(player, id, buf);
            }
        }
    }
}