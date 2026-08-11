package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.event.Event;

/**
 * An event published after a configuration value has been loaded from disk.
 *
 * @param unit the configuration that completed loading
 * @param value the loaded and validated value
 * @param <T> the type of the configuration value
 */
public record ConfigLoadEvent<T>(ConfigUnit<T> unit, T value) implements Event {
}
