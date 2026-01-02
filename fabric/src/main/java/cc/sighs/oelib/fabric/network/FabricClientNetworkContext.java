package cc.sighs.oelib.fabric.network;

import cc.sighs.oelib.network.api.INetworkContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric Client implementation of {@link INetworkContext}.
 */
public class FabricClientNetworkContext implements INetworkContext {

    private final ClientPlayNetworking.Context context;

    public FabricClientNetworkContext(ClientPlayNetworking.Context context) {
        this.context = context;
    }

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
    public Minecraft client() {
        return context.client();
    }

    @Override
    public void enqueueWork(Runnable task) {
        context.client().execute(task);
    }
}
