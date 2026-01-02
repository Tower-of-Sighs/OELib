package cc.sighs.oelib.config.spi;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;

public interface IConfigEventDispatcher {
    <T> void fireLoad(ConfigLoadEvent<T> event);

    <T> void fireBeforeSave(ConfigSaveEvent<T> event);

    <T> void fireAfterSave(ConfigSaveEvent<T> event);

    <T> void fireChanged(ConfigChangedEvent<T> event);

    <T> void fireSync(ConfigSyncEvent<T> event);
}

