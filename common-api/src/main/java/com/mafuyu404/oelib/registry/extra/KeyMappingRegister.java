package com.mafuyu404.oelib.registry.extra;

import com.mafuyu404.oelib.registry.RegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.KeyMappingAction;
import net.minecraft.client.KeyMapping;

public final class KeyMappingRegister {
    private KeyMappingRegister() {
    }

    public static void register(KeyMapping mapping) {
        RegistrationDispatcher.perform(new KeyMappingAction(mapping));
    }
}