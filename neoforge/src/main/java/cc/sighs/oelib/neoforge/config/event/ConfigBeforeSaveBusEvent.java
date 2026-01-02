package cc.sighs.oelib.neoforge.config.event;

import cc.sighs.oelib.config.api.ConfigSaveEvent;
import net.neoforged.bus.api.Event;

public final class ConfigBeforeSaveBusEvent<T> extends Event {
    private final ConfigSaveEvent<T> event;

    public ConfigBeforeSaveBusEvent(ConfigSaveEvent<T> event) {
        this.event = event;
    }

    public ConfigSaveEvent<T> event() {
        return event;
    }
}
