package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;

/**
 * Fired when a config value is synchronized across client/server.
 */
public record ConfigSyncEvent<T>(ConfigUnit<T> unit, T value, boolean fromServer) {
}
