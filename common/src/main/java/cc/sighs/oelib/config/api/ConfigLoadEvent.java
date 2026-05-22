package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;

/**
 * An event published after a configuration value has been loaded from disk.
 *
 * @param unit  the configuration unit that was loaded
 * @param value the loaded value
 * @param <T>   the type of the configuration value
 */
public record ConfigLoadEvent<T>(ConfigUnit<T> unit, T value) {
}
