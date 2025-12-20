package com.sighs.oelib.registry.spi;

import com.sighs.oelib.registry.action.RegistrationAction;

public interface IRegistrationDispatcher {
    void perform(RegistrationAction action);
}