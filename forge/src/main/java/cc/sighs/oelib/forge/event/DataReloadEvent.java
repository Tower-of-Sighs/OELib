package cc.sighs.oelib.forge.event;

import net.minecraftforge.eventbus.api.Event;

public class DataReloadEvent extends Event {

    private final Class<?> dataClass;
    private final int loadedCount;
    private final int invalidCount;

    public DataReloadEvent(Class<?> dataClass, int loadedCount, int invalidCount) {
        this.dataClass = dataClass;
        this.loadedCount = loadedCount;
        this.invalidCount = invalidCount;
    }

    public Class<?> getDataClass() {
        return dataClass;
    }

    public int getLoadedCount() {
        return loadedCount;
    }

    public int getInvalidCount() {
        return invalidCount;
    }

    public boolean isDataType(Class<?> clazz) {
        return dataClass.equals(clazz);
    }
}