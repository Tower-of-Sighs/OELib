package cc.sighs.oelib.event;

/**
 * Logical phase of an event handler within a single dispatch.
 * <p>
 * Handlers are ordered by phase first, then by {@link EventPriority}.
 */
public enum EventPhase {
    /**
     * Earliest phase, intended for validation and high-level veto logic.
     */
    PRE,
    /**
     * Default phase for most handlers.
     */
    NORMAL,
    /**
     * Latest phase, typically used for observing final state.
     */
    POST
}
