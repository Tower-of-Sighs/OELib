package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.spi.IConfigEventDispatcher;

import java.util.ServiceLoader;

/**
 * Static entry point for firing configuration lifecycle events.
 *
 * <p>{@code ConfigEvents} uses the {@link ServiceLoader} mechanism to
 * discover an {@link IConfigEventDispatcher} implementation at runtime.
 * If no dispatcher is registered on the classpath, all event methods
 * are no-ops.
 *
 * <p>Each method corresponds to a lifecycle phase:
 * <ul>
 *   <li>{@link #onLoad(ConfigUnit, Object)} — after a config is loaded from disk</li>
 *   <li>{@link #beforeSave(ConfigUnit, Object)} — before a config is persisted</li>
 *   <li>{@link #afterSave(ConfigUnit, Object)} — after a config is persisted</li>
 *   <li>{@link #onChanged(ConfigUnit, Object, Object)} — after the in-memory value changes</li>
 *   <li>{@link #onSync(ConfigUnit, Object, boolean)} — after a network sync</li>
 * </ul>
 */
public class ConfigEvents {
    private static final IConfigEventDispatcher DISPATCHER = ServiceLoader
            .load(IConfigEventDispatcher.class)
            .findFirst()
            .orElse(null);

    /**
     * Fires a load event.
     *
     * @param unit  the configuration unit that was loaded
     * @param value the loaded value
     * @param <T>   the type of the configuration value
     */
    public static <T> void onLoad(ConfigUnit<T> unit, T value) {
        if (DISPATCHER != null) {
            DISPATCHER.fireLoad(new ConfigLoadEvent<>(unit, value));
        }
    }

    /**
     * Fires a pre-save event.
     *
     * @param unit  the configuration unit about to be persisted
     * @param value the value that will be written
     * @param <T>   the type of the configuration value
     */
    public static <T> void beforeSave(ConfigUnit<T> unit, T value) {
        if (DISPATCHER != null) {
            DISPATCHER.fireBeforeSave(new ConfigSaveEvent<>(unit, value));
        }
    }

    /**
     * Fires a post-save event.
     *
     * @param unit  the configuration unit that was persisted
     * @param value the value that was written
     * @param <T>   the type of the configuration value
     */
    public static <T> void afterSave(ConfigUnit<T> unit, T value) {
        if (DISPATCHER != null) {
            DISPATCHER.fireAfterSave(new ConfigSaveEvent<>(unit, value));
        }
    }

    /**
     * Fires a change event.
     *
     * @param unit     the configuration unit whose value changed
     * @param oldValue the value before the change
     * @param newValue the value after the change
     * @param <T>      the type of the configuration value
     */
    public static <T> void onChanged(ConfigUnit<T> unit, T oldValue, T newValue) {
        if (DISPATCHER != null) {
            DISPATCHER.fireChanged(new ConfigChangedEvent<>(unit, oldValue, newValue));
        }
    }

    /**
     * Fires a synchronization event.
     *
     * @param unit       the configuration unit being synced
     * @param value      the synced value
     * @param fromServer {@code true} if the sync originated from the server
     * @param <T>        the type of the configuration value
     */
    public static <T> void onSync(ConfigUnit<T> unit, T value, boolean fromServer) {
        if (DISPATCHER != null) {
            DISPATCHER.fireSync(new ConfigSyncEvent<>(unit, value, fromServer));
        }
    }
}
