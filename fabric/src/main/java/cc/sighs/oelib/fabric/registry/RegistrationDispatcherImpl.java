package cc.sighs.oelib.fabric.registry;

import cc.sighs.oelib.registry.action.RegistrationAction;
import cc.sighs.oelib.registry.spi.IRegistrationDispatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public final class RegistrationDispatcherImpl implements IRegistrationDispatcher {

    private final ServerRegistrationHandler serverHandler = new ServerRegistrationHandler();
    private final ClientRegistrationHandler clientHandler = new ClientRegistrationHandler();

    @Override
    public void perform(RegistrationAction action) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            clientHandler.perform(action);
        }
        serverHandler.perform(action);
    }
}