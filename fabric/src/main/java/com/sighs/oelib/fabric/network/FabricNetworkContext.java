package com.sighs.oelib.fabric.network;

import com.sighs.oelib.network.api.INetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric implementation of {@link INetworkContext}.
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

    @Override
    public void enqueueWork(Runnable task) {
        if (isServerSide) {
            ServerPlayer player = sender;
            if (player != null && player.serverLevel() != null) {
                player.serverLevel().getServer().execute(task);
            }
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(task);
        }
    }
}
