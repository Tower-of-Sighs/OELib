package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;

/**
 * An event published when a configuration value is modified, whether the
 * change originates from the GUI, a disk reload, or a network update.
 *
 * @param unit     the configuration unit whose value changed
 * @param oldValue the value before the change
 * @param newValue the value after the change
 * @param <T>      the type of the configuration value
 */
public record ConfigChangedEvent<T>(ConfigUnit<T> unit, T oldValue, T newValue) {
}
