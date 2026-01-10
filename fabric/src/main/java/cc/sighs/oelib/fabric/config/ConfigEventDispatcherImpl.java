package cc.sighs.oelib.fabric.config;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;
import cc.sighs.oelib.config.spi.IConfigEventDispatcher;
import cc.sighs.oelib.fabric.config.event.FabricConfigEvent;

public class ConfigEventDispatcherImpl implements IConfigEventDispatcher {

    @Override
    public <T> void fireLoad(ConfigLoadEvent<T> event) {
        FabricConfigEvent.LOAD.invoker().onLoad(event);
    }

    @Override
    public <T> void fireBeforeSave(ConfigSaveEvent<T> event) {
        FabricConfigEvent.BEFORE_SAVE.invoker().beforeSave(event);
    }

    @Override
    public <T> void fireAfterSave(ConfigSaveEvent<T> event) {
        FabricConfigEvent.AFTER_SAVE.invoker().afterSave(event);
    }

    @Override
    public <T> void fireChanged(ConfigChangedEvent<T> event) {
        FabricConfigEvent.CHANGED.invoker().onChanged(event);
    }

    @Override
    public <T> void fireSync(ConfigSyncEvent<T> event) {
        FabricConfigEvent.SYNC.invoker().onSync(event);
    }
}