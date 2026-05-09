package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;

/**
 * An event published when a configuration value is synchronized between
 * the client and the server.
 *
 * @param unit       the configuration unit being synchronized
 * @param value      the synchronized value
 * @param fromServer {@code true} if the sync originated from the server,
 *                   {@code false} if it originated from the client
 * @param <T>        the type of the configuration value
 */
public record ConfigSyncEvent<T>(ConfigUnit<T> unit, T value, boolean fromServer) {
}
