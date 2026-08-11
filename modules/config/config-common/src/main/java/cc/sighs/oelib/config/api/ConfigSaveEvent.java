package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.event.Event;

/**
 * Events published before and after a configuration value is persisted to disk.
 *
 * @param <T> the type of the configuration value
 */
public abstract class ConfigSaveEvent<T> implements Event {
    private final ConfigUnit<T> unit;
    private final T value;

    /**
     * Creates a persistence event.
     *
     * @param unit the configuration being persisted
     * @param value the value supplied to the persistence operation
     */
    protected ConfigSaveEvent(ConfigUnit<T> unit, T value) {
        this.unit = unit;
        this.value = value;
    }

    /**
     * Returns the configuration unit being persisted.
     *
     * @return the unit
     */
    public ConfigUnit<T> unit() {
        return unit;
    }

    /**
     * Returns the value being written.
     *
     * @return the value
     */
    public T value() {
        return value;
    }

    /**
     * Indicates that a configuration value is about to be persisted.
     *
     * @param <T> the type of the configuration value
     */
    public static final class Pre<T> extends ConfigSaveEvent<T> {
        /**
         * Creates a pre-persistence event.
         *
         * @param unit the configuration being persisted
         * @param value the value supplied to the persistence operation
         */
        public Pre(ConfigUnit<T> unit, T value) {
            super(unit, value);
        }
    }

    /**
     * Indicates that a configuration value was successfully persisted.
     *
     * @param <T> the type of the configuration value
     */
    public static final class Post<T> extends ConfigSaveEvent<T> {
        /**
         * Creates a post-persistence event.
         *
         * @param unit the configuration that was persisted
         * @param value the persisted value
         */
        public Post(ConfigUnit<T> unit, T value) {
            super(unit, value);
        }
    }
}
