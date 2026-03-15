package cc.sighs.oelib.forge.config.event;

import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.config.api.ConfigLoadEvent;
import cc.sighs.oelib.config.api.ConfigSaveEvent;
import cc.sighs.oelib.config.api.ConfigSyncEvent;
import net.minecraftforge.eventbus.api.Event;

public abstract class ForgeConfigEvent<E> extends Event {
    private final E event;

    protected ForgeConfigEvent(E event) {
        this.event = event;
    }

    public E get() {
        return event;
    }

    public static final class Load<T> extends ForgeConfigEvent<ConfigLoadEvent<T>> {
        public Load(ConfigLoadEvent<T> event) {
            super(event);
        }
    }

    public static final class BeforeSave<T> extends ForgeConfigEvent<ConfigSaveEvent<T>> {
        public BeforeSave(ConfigSaveEvent<T> event) {
            super(event);
        }
    }

    public static final class AfterSave<T> extends ForgeConfigEvent<ConfigSaveEvent<T>> {
        public AfterSave(ConfigSaveEvent<T> event) {
            super(event);
        }
    }

    public static final class Changed<T> extends ForgeConfigEvent<ConfigChangedEvent<T>> {
        public Changed(ConfigChangedEvent<T> event) {
            super(event);
        }
    }

    public static final class Sync<T> extends ForgeConfigEvent<ConfigSyncEvent<T>> {
        public Sync(ConfigSyncEvent<T> event) {
            super(event);
        }
    }
}
