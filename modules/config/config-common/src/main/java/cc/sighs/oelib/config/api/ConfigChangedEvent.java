package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.event.Event;

/**
 * An event published when a configuration value is modified, whether the
 * change originates from the GUI, a disk reload, or a network update.
 *
 * @param unit the configuration whose accepted value changed
 * @param oldValue the value before the change
 * @param newValue the accepted value after the change
 * @param <T> the type of the configuration value
 */
public record ConfigChangedEvent<T>(ConfigUnit<T> unit, T oldValue, T newValue) implements Event {
}
