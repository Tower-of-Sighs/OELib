package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.event.Event;

/**
 * An event published when a configuration value is modified, whether the
 * change originates from the GUI, a disk reload, or a network update.
 *
 * @param <T> the type of the configuration value
 */
public record ConfigChangedEvent<T>(ConfigUnit<T> unit, T oldValue, T newValue) implements Event {
}
