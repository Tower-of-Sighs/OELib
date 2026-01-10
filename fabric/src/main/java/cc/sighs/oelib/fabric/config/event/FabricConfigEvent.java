package cc.sighs.oelib.fabric.config.event;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class FabricConfigEvent {

    public static final Event<LoadCallback> LOAD = EventFactory.createArrayBacked(LoadCallback.class,
            listeners -> event -> {
                for (LoadCallback l : listeners) l.onLoad(event);
            });
    public static final Event<BeforeSaveCallback> BEFORE_SAVE = EventFactory.createArrayBacked(BeforeSaveCallback.class,
            listeners -> event -> {
                for (BeforeSaveCallback l : listeners) l.beforeSave(event);
            });
    public static final Event<AfterSaveCallback> AFTER_SAVE = EventFactory.createArrayBacked(AfterSaveCallback.class,
            listeners -> event -> {
                for (AfterSaveCallback l : listeners) l.afterSave(event);
            });
    public static final Event<ChangedCallback> CHANGED = EventFactory.createArrayBacked(ChangedCallback.class,
            listeners -> event -> {
                for (ChangedCallback l : listeners) l.onChanged(event);
            });
    public static final Event<SyncCallback> SYNC = EventFactory.createArrayBacked(SyncCallback.class,
            listeners -> event -> {
                for (SyncCallback l : listeners) l.onSync(event);
            });

    private FabricConfigEvent() {}

    @FunctionalInterface
    public interface LoadCallback {
        void onLoad(ConfigLoadEvent<?> event);
    }

    @FunctionalInterface
    public interface BeforeSaveCallback {
        void beforeSave(ConfigSaveEvent<?> event);
    }

    @FunctionalInterface
    public interface AfterSaveCallback {
        void afterSave(ConfigSaveEvent<?> event);
    }

    @FunctionalInterface
    public interface ChangedCallback {
        void onChanged(ConfigChangedEvent<?> event);
    }

    @FunctionalInterface
    public interface SyncCallback {
        void onSync(ConfigSyncEvent<?> event);
    }
}