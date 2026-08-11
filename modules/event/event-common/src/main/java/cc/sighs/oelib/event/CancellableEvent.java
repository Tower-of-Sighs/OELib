package cc.sighs.oelib.event;

/**
 * Event type that supports cooperative cancellation.
 * <p>
 * {@link EventBus} will inspect this interface at dispatch time and skip
 * handlers that do not wish to receive cancelled events, based on
 * {@link Subscribe#receiveCanceled()}.
 * <p>
 * Implementations are expected to be used from a single thread (typically the
 * main game thread); {@link #setCanceled(boolean)} is not synchronized.
 */
public interface CancellableEvent extends Event {
    boolean isCanceled();

    void setCanceled(boolean canceled);

    default void cancel() {
        setCanceled(true);
    }
}
