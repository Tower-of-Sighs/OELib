package cc.sighs.oelib.event;

/**
 * Relative priority of handlers for a given event type.
 * <p>
 * Higher priorities are invoked before lower priorities.
 */
public enum EventPriority {
    /**
     * Highest priority; runs before all other priorities.
     */
    HIGHEST,
    /**
     * High priority; runs after {@link #HIGHEST} handlers.
     */
    HIGH,
    /**
     * Default priority for most handlers.
     */
    NORMAL,
    /**
     * Low priority; runs after {@link #NORMAL} handlers.
     */
    LOW,
    /**
     * Lowest priority; runs last.
     */
    LOWEST
}
