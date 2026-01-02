package cc.sighs.oelib.neoforge.config.event;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import net.neoforged.bus.api.Event;

public final class ConfigChangedBusEvent<T> extends Event {
    private final ConfigChangedEvent<T> event;

    public ConfigChangedBusEvent(ConfigChangedEvent<T> event) {
        this.event = event;
    }

    public ConfigChangedEvent<T> event() {
        return event;
    }
}
