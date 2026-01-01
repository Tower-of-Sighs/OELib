package com.sighs.oelib.registry.extra;

import com.sighs.oelib.registry.RegistrationDispatcher;
import com.sighs.oelib.registry.action.CommandRegisterAction;

public final class CommandRegister {
    private CommandRegister() {
    }

    public static void registerServer(CommandRegisterAction.CommandRegistrar registrar) {
        RegistrationDispatcher.perform(new CommandRegisterAction(registrar, false));
    }

    public static void registerClient(CommandRegisterAction.CommandRegistrar registrar) {
        RegistrationDispatcher.perform(new CommandRegisterAction(registrar, true));
    }
}

