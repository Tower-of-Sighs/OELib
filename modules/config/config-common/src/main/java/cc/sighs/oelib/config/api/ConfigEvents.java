package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.event.EventBus;

/**
 * Static entry point for firing configuration lifecycle events on the
 * {@link EventBus}.
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
public final class ConfigEvents {
    private ConfigEvents() {
    }

    /**
     * Fires a load event.
     *
     * @param unit  the configuration unit that was loaded
     * @param value the loaded value
     * @param <T>   the type of the configuration value
     */
    public static <T> void onLoad(ConfigUnit<T> unit, T value) {
        EventBus.post(new ConfigLoadEvent<>(unit, value));
    }

    /**
     * Fires a pre-save event.
     *
     * @param unit  the configuration unit about to be persisted
     * @param value the value that will be written
     * @param <T>   the type of the configuration value
     */
    public static <T> void beforeSave(ConfigUnit<T> unit, T value) {
        EventBus.post(new ConfigSaveEvent.Pre<>(unit, value));
    }

    /**
     * Fires a post-save event.
     *
     * @param unit  the configuration unit that was persisted
     * @param value the value that was written
     * @param <T>   the type of the configuration value
     */
    public static <T> void afterSave(ConfigUnit<T> unit, T value) {
        EventBus.post(new ConfigSaveEvent.Post<>(unit, value));
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
        EventBus.post(new ConfigChangedEvent<>(unit, oldValue, newValue));
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
        EventBus.post(new ConfigSyncEvent<>(unit, value, fromServer));
    }
}
