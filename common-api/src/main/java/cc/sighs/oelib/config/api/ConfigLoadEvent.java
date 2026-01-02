package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;

/**
 * Fired after a config value is loaded from disk.
 */
public record ConfigLoadEvent<T>(ConfigUnit<T> unit, T value) {
}
