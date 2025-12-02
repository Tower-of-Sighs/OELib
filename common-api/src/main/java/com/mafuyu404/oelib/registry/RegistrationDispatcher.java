package com.mafuyu404.oelib.registry;

import com.mafuyu404.oelib.api.registry.IRegistrationDispatcher;
import com.mafuyu404.oelib.registry.action.RegistrationAction;

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