package cc.sighs.oelib.fabric.config.event;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;
import cc.sighs.oelib.config.spi.IConfigEventDispatcher;
import cc.sighs.oelib.fabric.config.event.util.EventUtils;
import net.fabricmc.fabric.api.event.Event;

public class ConfigEventDispatcherImpl implements IConfigEventDispatcher {

    public static final Event<LoadCallback> LOAD_EVENT =
            EventUtils.arrayBacked(LoadCallback.class, listeners -> event -> {
                for (LoadCallback l : listeners) l.onLoad(event);
            });
    public static final Event<BeforeSaveCallback> BEFORE_SAVE_EVENT =
            EventUtils.arrayBacked(BeforeSaveCallback.class, listeners -> event -> {
                for (BeforeSaveCallback l : listeners) l.beforeSave(event);
            });
    public static final Event<AfterSaveCallback> AFTER_SAVE_EVENT =
            EventUtils.arrayBacked(AfterSaveCallback.class, listeners -> event -> {
                for (AfterSaveCallback l : listeners) l.afterSave(event);
            });
    public static final Event<ChangedCallback> CHANGED_EVENT =
            EventUtils.arrayBacked(ChangedCallback.class, listeners -> event -> {
                for (ChangedCallback l : listeners) l.onChanged(event);
            });
    public static final Event<SyncCallback> SYNC_EVENT =
            EventUtils.arrayBacked(SyncCallback.class, listeners -> event -> {
                for (SyncCallback l : listeners) l.onSync(event);
            });

    @Override
    public <T> void fireLoad(ConfigLoadEvent<T> event) {
        LOAD_EVENT.invoker().onLoad(event);
    }

    @Override
    public <T> void fireBeforeSave(ConfigSaveEvent<T> event) {
        BEFORE_SAVE_EVENT.invoker().beforeSave(event);
    }

    @Override
    public <T> void fireAfterSave(ConfigSaveEvent<T> event) {
        AFTER_SAVE_EVENT.invoker().afterSave(event);
    }

    @Override
    public <T> void fireChanged(ConfigChangedEvent<T> event) {
        CHANGED_EVENT.invoker().onChanged(event);
    }

    @Override
    public <T> void fireSync(ConfigSyncEvent<T> event) {
        SYNC_EVENT.invoker().onSync(event);
    }

    public interface LoadCallback {
        void onLoad(ConfigLoadEvent<?> event);
    }

    public interface BeforeSaveCallback {
        void beforeSave(ConfigSaveEvent<?> event);
    }

    public interface AfterSaveCallback {
        void afterSave(ConfigSaveEvent<?> event);
    }

    public interface ChangedCallback {
        void onChanged(ConfigChangedEvent<?> event);
    }

    public interface SyncCallback {
        void onSync(ConfigSyncEvent<?> event);
    }
}
