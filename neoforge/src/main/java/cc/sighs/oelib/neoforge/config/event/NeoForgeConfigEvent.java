package cc.sighs.oelib.neoforge.config.event;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;
import net.neoforged.bus.api.Event;

public abstract class NeoForgeConfigEvent<E> extends Event {
    private final E event;

    protected NeoForgeConfigEvent(E event) {
        this.event = event;
    }

    public E get() {
        return event;
    }

    public static final class Load<T> extends NeoForgeConfigEvent<ConfigLoadEvent<T>> {
        public Load(ConfigLoadEvent<T> event) {
            super(event);
        }
    }

    public static final class BeforeSave<T> extends NeoForgeConfigEvent<ConfigSaveEvent<T>> {
        public BeforeSave(ConfigSaveEvent<T> event) {
            super(event);
        }
    }

    public static final class AfterSave<T> extends NeoForgeConfigEvent<ConfigSaveEvent<T>> {
        public AfterSave(ConfigSaveEvent<T> event) {
            super(event);
        }
    }

    public static final class Changed<T> extends NeoForgeConfigEvent<ConfigChangedEvent<T>> {
        public Changed(ConfigChangedEvent<T> event) {
            super(event);
        }
    }

    public static final class Sync<T> extends NeoForgeConfigEvent<ConfigSyncEvent<T>> {
        public Sync(ConfigSyncEvent<T> event) {
            super(event);
        }
    }
}