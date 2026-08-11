package cc.sighs.oelib.registry.forge;

import cc.sighs.oelib.registry.action.RegistrationAction;
import cc.sighs.oelib.registry.spi.IRegistrationDispatcher;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class RegistrationDispatcherImpl implements IRegistrationDispatcher {

    private final ServerRegistrationHandler serverHandler = new ServerRegistrationHandler();
    private final ClientRegistrationHandler clientHandler = new ClientRegistrationHandler();

    @Override
    public void perform(RegistrationAction action) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            clientHandler.perform(action);
        }
        serverHandler.perform(action);
    }

    public static final class ClientEventHooks extends ClientRegistrationHandler.ClientEventHooks {
    }

    public static final class ModEventHooks extends ServerRegistrationHandler.ModEventHooks {
    }
}