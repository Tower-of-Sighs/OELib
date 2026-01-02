package cc.sighs.oelib.neoforge.config.event;

import cc.sighs.oelib.config.api.ConfigSaveEvent;
import net.neoforged.bus.api.Event;

public final class ConfigAfterSaveBusEvent<T> extends Event {
    private final ConfigSaveEvent<T> event;

    public ConfigAfterSaveBusEvent(ConfigSaveEvent<T> event) {
        this.event = event;
    }

    public ConfigSaveEvent<T> event() {
        return event;
    }
}
