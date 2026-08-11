package cc.sighs.oelib.registry.extra;

import cc.sighs.oelib.registry.RegistrationDispatcher;
import cc.sighs.oelib.registry.action.KeyMappingAction;
import net.minecraft.client.KeyMapping;

public final class KeyMappingRegister {
    private KeyMappingRegister() {
    }

    public static void register(KeyMapping mapping) {
        RegistrationDispatcher.perform(new KeyMappingAction(mapping));
    }
}