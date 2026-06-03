package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.event.Event;

/**
 * An event published when a configuration value is synchronized between
 * the client and the server.
 *
 * @param <T> the type of the configuration value
 */
public record ConfigSyncEvent<T>(ConfigUnit<T> unit, T value, boolean fromServer) implements Event {
}
