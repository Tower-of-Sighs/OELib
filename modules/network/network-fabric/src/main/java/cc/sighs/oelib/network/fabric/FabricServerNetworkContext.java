package cc.sighs.oelib.network.fabric;

import cc.sighs.oelib.network.api.INetworkContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric Server implementation of {@link INetworkContext}.
 */
public class FabricServerNetworkContext implements INetworkContext {

    private final ServerPlayNetworking.Context context;

    public FabricServerNetworkContext(ServerPlayNetworking.Context context) {
        this.context = context;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public boolean isServerSide() {
        return true;
    }

    @Override
    public ServerPlayer sender() {
        return context.player();
    }

    @Override
    public Minecraft client() {
        return null;
    }

    @Override
    public void enqueueWork(Runnable task) {
        context.server().execute(task);
    }

    @Override
    public RegistryAccess registryAccess() {
        return context.player().registryAccess();
    }
}
