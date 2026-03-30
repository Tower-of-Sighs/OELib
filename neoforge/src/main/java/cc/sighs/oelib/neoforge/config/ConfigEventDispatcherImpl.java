package cc.sighs.oelib.neoforge.config;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;
import cc.sighs.oelib.config.spi.IConfigEventDispatcher;
import cc.sighs.oelib.neoforge.config.event.NeoForgeConfigEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ConfigEventDispatcherImpl implements IConfigEventDispatcher {

    @Override
    public <T> void fireLoad(ConfigLoadEvent<T> event) {
        NeoForge.EVENT_BUS.post(new NeoForgeConfigEvent.Load<>(event));
    }

    @Override
    public <T> void fireBeforeSave(ConfigSaveEvent<T> event) {
        NeoForge.EVENT_BUS.post(new NeoForgeConfigEvent.BeforeSave<>(event));
    }

    @Override
    public <T> void fireAfterSave(ConfigSaveEvent<T> event) {
        NeoForge.EVENT_BUS.post(new NeoForgeConfigEvent.AfterSave<>(event));
    }

    @Override
    public <T> void fireChanged(ConfigChangedEvent<T> event) {
        NeoForge.EVENT_BUS.post(new NeoForgeConfigEvent.Changed<>(event));
    }

    @Override
    public <T> void fireSync(ConfigSyncEvent<T> event) {
        NeoForge.EVENT_BUS.post(new NeoForgeConfigEvent.Sync<>(event));
    }
}