package cc.sighs.oelib.event.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface DataReloadEvent {

    Event<DataReloadEvent> EVENT = EventFactory.createArrayBacked(DataReloadEvent.class,
            (listeners) -> (dataClass, loadedCount, invalidCount) -> {
                for (DataReloadEvent listener : listeners) {
                    listener.onDataReload(dataClass, loadedCount, invalidCount);
                }
            });

    void onDataReload(Class<?> dataClass, int loadedCount, int invalidCount);
}