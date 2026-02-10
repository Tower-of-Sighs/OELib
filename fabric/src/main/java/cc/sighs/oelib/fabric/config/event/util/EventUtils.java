package cc.sighs.oelib.fabric.config.event.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.function.Function;

public final class EventUtils {
    private EventUtils() {
    }

    public static <C> Event<C> arrayBacked(Class<C> callbackType, Function<C[], C> composer) {
        return EventFactory.createArrayBacked(callbackType, composer);
    }
}
