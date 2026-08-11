package cc.sighs.oelib.registry.spi;

import cc.sighs.oelib.registry.action.RegistrationAction;

public interface IRegistrationDispatcher {
    void perform(RegistrationAction action);
}