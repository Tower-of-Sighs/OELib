package cc.sighs.oelib.neoforge.config.event;

import cc.sighs.oelib.config.api.ConfigSyncEvent;
import net.neoforged.bus.api.Event;

public final class ConfigSyncBusEvent<T> extends Event {
    private final ConfigSyncEvent<T> event;

    public ConfigSyncBusEvent(ConfigSyncEvent<T> event) {
        this.event = event;
    }

    public ConfigSyncEvent<T> event() {
        return event;
    }
}
