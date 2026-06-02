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
     * Fired before the value is written to disk.
     */
    public static final class Pre<T> extends ConfigSaveEvent<T> {
        public Pre(ConfigUnit<T> unit, T value) {
            super(unit, value);
        }
    }

    /**
     * Fired after the value has been written to disk.
     */
    public static final class Post<T> extends ConfigSaveEvent<T> {
        public Post(ConfigUnit<T> unit, T value) {
            super(unit, value);
        }
    }
}
