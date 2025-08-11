package com.mafuyu404.oelib.fabric.network;

import com.mafuyu404.oelib.api.net.INetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric网络上下文实现。
 */
public record FabricNetworkContext(ServerPlayer sender, boolean isServerSide) implements INetworkContext {

    @Override
    public boolean isClientSide() {
        return !isServerSide;
    }

    @Override
    public Minecraft client() {
        if (isClientSide()) {
            return Minecraft.getInstance();
        }
        return null;
    }
}