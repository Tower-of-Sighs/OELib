package cc.sighs.oelib.config.spi;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;

/**
 * Service-provider interface for receiving configuration lifecycle events.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} and
 * used by {@link cc.sighs.oelib.config.api.ConfigEvents ConfigEvents} to
 * dispatch load, save, change, and sync events to platform-specific
 * event buses (for example, NeoForge or Fabric events).
 *
 * <p>Each method receives a typed event record. Implementations must be
 * thread-safe, as events may be fired from network threads during
 * synchronization.
 */
public interface IConfigEventDispatcher {
    /**
     * Fires a load event after a configuration is read from disk.
     *
     * @param event the load event
     * @param <T>   the configuration value type
     */
    <T> void fireLoad(ConfigLoadEvent<T> event);

    /**
     * Fires a pre-save event before a configuration is written to disk.
     *
     * @param event the save event
     * @param <T>   the configuration value type
     */
    <T> void fireBeforeSave(ConfigSaveEvent<T> event);

    /**
     * Fires a post-save event after a configuration has been written to disk.
     *
     * @param event the save event
     * @param <T>   the configuration value type
     */
    <T> void fireAfterSave(ConfigSaveEvent<T> event);

    /**
     * Fires a change event when a configuration's in-memory value is
     * modified.
     *
     * @param event the change event
     * @param <T>   the configuration value type
     */
    <T> void fireChanged(ConfigChangedEvent<T> event);

    /**
     * Fires a sync event when a configuration value is synchronized over
     * the network.
     *
     * @param event the sync event
     * @param <T>   the configuration value type
     */
    <T> void fireSync(ConfigSyncEvent<T> event);
}
