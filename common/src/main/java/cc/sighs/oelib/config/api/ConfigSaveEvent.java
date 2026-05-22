package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;

/**
 * An event published around configuration persistence.
 *
 * <p>This event is fired twice per save operation: once before the value
 * is written to disk (via
 * {@link ConfigEvents#beforeSave(ConfigUnit, Object)}) and once after
 * the write completes (via
 * {@link ConfigEvents#afterSave(ConfigUnit, Object)}).
 *
 * @param unit  the configuration unit being persisted
 * @param value the value being written
 * @param <T>   the type of the configuration value
 */
public record ConfigSaveEvent<T>(ConfigUnit<T> unit, T value) {
}
