package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;

/**
 * Fired when a config value changes (GUI, disk, or network).
 */
public record ConfigChangedEvent<T>(ConfigUnit<T> unit, T oldValue, T newValue) {
}
