package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;

/**
 * Fired before and after a config value is persisted.
 */
public record ConfigSaveEvent<T>(ConfigUnit<T> unit, T value) {
}
