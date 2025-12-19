package com.sighs.oelib.registry.extra;

import com.sighs.oelib.registry.RegistrationDispatcher;
import com.sighs.oelib.registry.action.KeyMappingAction;
import net.minecraft.client.KeyMapping;

public final class KeyMappingRegister {
    private KeyMappingRegister() {
    }

    public static void register(KeyMapping mapping) {
        RegistrationDispatcher.perform(new KeyMappingAction(mapping));
    }
}