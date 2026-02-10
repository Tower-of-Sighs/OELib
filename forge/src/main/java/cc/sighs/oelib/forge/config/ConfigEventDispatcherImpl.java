package cc.sighs.oelib.forge.config;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;
import cc.sighs.oelib.config.spi.IConfigEventDispatcher;
import cc.sighs.oelib.forge.config.event.ForgeConfigEvent;
import net.minecraftforge.common.MinecraftForge;

public class ConfigEventDispatcherImpl implements IConfigEventDispatcher {

    @Override
    public <T> void fireLoad(ConfigLoadEvent<T> event) {
        MinecraftForge.EVENT_BUS.post(new ForgeConfigEvent.Load<>(event));
    }

    @Override
    public <T> void fireBeforeSave(ConfigSaveEvent<T> event) {
        MinecraftForge.EVENT_BUS.post(new ForgeConfigEvent.BeforeSave<>(event));
    }

    @Override
    public <T> void fireAfterSave(ConfigSaveEvent<T> event) {
        MinecraftForge.EVENT_BUS.post(new ForgeConfigEvent.AfterSave<>(event));
    }

    @Override
    public <T> void fireChanged(ConfigChangedEvent<T> event) {
        MinecraftForge.EVENT_BUS.post(new ForgeConfigEvent.Changed<>(event));
    }

    @Override
    public <T> void fireSync(ConfigSyncEvent<T> event) {
        MinecraftForge.EVENT_BUS.post(new ForgeConfigEvent.Sync<>(event));
    }
}