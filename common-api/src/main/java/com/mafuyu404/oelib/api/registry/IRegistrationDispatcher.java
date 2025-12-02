package com.mafuyu404.oelib.api.registry;

import com.mafuyu404.oelib.registry.action.RegistrationAction;

public interface IRegistrationDispatcher {
    void perform(RegistrationAction action);
}