package cc.sighs.oelib.forge.network;

import cc.sighs.oelib.network.api.INetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Forge网络上下文实现。
 */
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
        if (isClientSide()) {
            return Minecraft.getInstance();
        }
        return null;
    }
}