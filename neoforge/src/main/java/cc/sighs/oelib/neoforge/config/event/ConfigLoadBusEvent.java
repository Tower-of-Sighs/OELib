package cc.sighs.oelib.neoforge.config.event;

import cc.sighs.oelib.config.api.ConfigLoadEvent;
import net.neoforged.bus.api.Event;

public final class ConfigLoadBusEvent<T> extends Event {
    private final ConfigLoadEvent<T> event;

    public ConfigLoadBusEvent(ConfigLoadEvent<T> event) {
        this.event = event;
    }

    public ConfigLoadEvent<T> event() {
        return event;
    }
}
