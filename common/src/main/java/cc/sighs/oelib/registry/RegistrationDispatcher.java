package cc.sighs.oelib.registry;

import cc.sighs.oelib.registry.action.RegistrationAction;
import cc.sighs.oelib.registry.spi.IRegistrationDispatcher;

import java.util.ServiceLoader;

public final class RegistrationDispatcher {
    private static final IRegistrationDispatcher IMPL;

    static {
        IMPL = ServiceLoader.load(IRegistrationDispatcher.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No IRegistrationDispatcher implementation found"));
    }

    private RegistrationDispatcher() {
    }

    public static void perform(RegistrationAction action) {
        IMPL.perform(action);
    }
}