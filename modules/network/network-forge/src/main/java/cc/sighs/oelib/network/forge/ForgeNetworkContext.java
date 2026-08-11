package cc.sighs.oelib.network.forge;

import cc.sighs.oelib.network.api.INetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class ForgeNetworkContext implements INetworkContext {
    private final NetworkEvent.Context context;

    public ForgeNetworkContext(NetworkEvent.Context context) {
        this.context = context;
    }

    @Override
    public boolean isClientSide() {
        return context.getDirection().getReceptionSide().isClient();
    }

    @Override
    public boolean isServerSide() {
        return context.getDirection().getReceptionSide().isServer();
    }

    @Override
    public ServerPlayer sender() {
        return context.getSender();
    }

    @Override
    public Minecraft client() {
        return Minecraft.getInstance();
    }

    @Override
    public void enqueueWork(Runnable task) {
        context.enqueueWork(task);
    }

    @Override
    public RegistryAccess registryAccess() {
        if (isServerSide() && context.getSender() != null) {
            return context.getSender().getCommandSenderWorld().registryAccess();
        }
        return Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess()
                : RegistryAccess.EMPTY;
    }
}
