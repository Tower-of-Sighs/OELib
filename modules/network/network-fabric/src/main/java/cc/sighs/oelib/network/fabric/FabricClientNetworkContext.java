package cc.sighs.oelib.network.fabric;

import cc.sighs.oelib.network.api.INetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric Client implementation of {@link INetworkContext}.
 */
public record FabricClientNetworkContext(Minecraft client) implements INetworkContext {

    @Override
    public boolean isClientSide() {
        return true;
    }

    @Override
    public boolean isServerSide() {
        return false;
    }

    @Override
    public ServerPlayer sender() {
        return null;
    }

    @Override
    public void enqueueWork(Runnable task) {
        client.execute(task);
    }

    @Override
    public RegistryAccess registryAccess() {
        return client.level != null ? client.level.registryAccess() : RegistryAccess.EMPTY;
    }
}
