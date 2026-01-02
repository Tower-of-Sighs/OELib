package cc.sighs.oelib.neoforge.config;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;
import cc.sighs.oelib.config.spi.IConfigEventDispatcher;
import cc.sighs.oelib.neoforge.config.event.*;
import net.neoforged.neoforge.common.NeoForge;

public class ConfigEventDispatcherImpl implements IConfigEventDispatcher {
    @Override
    public <T> void fireLoad(ConfigLoadEvent<T> event) {
        NeoForge.EVENT_BUS.post(new ConfigLoadBusEvent<>(event));
    }

    @Override
    public <T> void fireBeforeSave(ConfigSaveEvent<T> event) {
        NeoForge.EVENT_BUS.post(new ConfigBeforeSaveBusEvent<>(event));
    }

    @Override
    public <T> void fireAfterSave(ConfigSaveEvent<T> event) {
        NeoForge.EVENT_BUS.post(new ConfigAfterSaveBusEvent<>(event));
    }

    @Override
    public <T> void fireChanged(ConfigChangedEvent<T> event) {
        NeoForge.EVENT_BUS.post(new ConfigChangedBusEvent<>(event));
    }

    @Override
    public <T> void fireSync(ConfigSyncEvent<T> event) {
        NeoForge.EVENT_BUS.post(new ConfigSyncBusEvent<>(event));
    }
}
