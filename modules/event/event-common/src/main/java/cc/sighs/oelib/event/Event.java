package cc.sighs.oelib.event;

/**
 * Marker interface for all events dispatched through the {@link EventBus}.
 * <p>
 * Any type used with {@link EventBus#post(Event)} must implement this interface.
 * Events are treated as plain data objects; OELib does not impose any inheritance
 * hierarchy beyond this marker.
 */
public interface Event {
}
