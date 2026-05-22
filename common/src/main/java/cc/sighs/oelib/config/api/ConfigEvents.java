package cc.sighs.oelib.config.api;

import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.spi.IConfigEventDispatcher;

import java.util.ServiceLoader;

public class ConfigEvents {
    private static final IConfigEventDispatcher DISPATCHER = ServiceLoader
            .load(IConfigEventDispatcher.class)
            .findFirst()
            .orElse(null);

    public static <T> void onLoad(ConfigUnit<T> unit, T value) {
        if (DISPATCHER != null) {
            DISPATCHER.fireLoad(new ConfigLoadEvent<>(unit, value));
        }
    }

    public static <T> void beforeSave(ConfigUnit<T> unit, T value) {
        if (DISPATCHER != null) {
            DISPATCHER.fireBeforeSave(new ConfigSaveEvent<>(unit, value));
        }
    }

    public static <T> void afterSave(ConfigUnit<T> unit, T value) {
        if (DISPATCHER != null) {
            DISPATCHER.fireAfterSave(new ConfigSaveEvent<>(unit, value));
        }
    }

    public static <T> void onChanged(ConfigUnit<T> unit, T oldValue, T newValue) {
        if (DISPATCHER != null) {
            DISPATCHER.fireChanged(new ConfigChangedEvent<>(unit, oldValue, newValue));
        }
    }

    public static <T> void onSync(ConfigUnit<T> unit, T value, boolean fromServer) {
        if (DISPATCHER != null) {
            DISPATCHER.fireSync(new ConfigSyncEvent<>(unit, value, fromServer));
        }
    }
}
