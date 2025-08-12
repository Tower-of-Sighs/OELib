package com.mafuyu404.oelib.neoforge.network;

import com.mafuyu404.oelib.api.net.INetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * NeoForge网络上下文实现。
 */
public class NeoForgeNetworkContext implements INetworkContext {

    private final IPayloadContext context;

    public NeoForgeNetworkContext(IPayloadContext context) {
        this.context = context;
    }

    @Override
    public boolean isClientSide() {
        return context.flow().isClientbound();
    }

    @Override
    public boolean isServerSide() {
        return context.flow().isServerbound();
    }

    @Override
    public ServerPlayer sender() {
        if (isServerSide() && context.player() instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        return null;
    }

    @Override
    public Minecraft client() {
        if (isClientSide()) {
            return Minecraft.getInstance();
        }
        return null;
    }
}