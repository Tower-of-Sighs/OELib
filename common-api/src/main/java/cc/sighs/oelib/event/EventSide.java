package cc.sighs.oelib.event;

/**
 * Logical side where a handler should be active.
 * <p>
 * This is evaluated at registration time using {@code Platform.isClient()}
 * and {@code Platform.isServer()}, so a handler may be registered on one
 * side but not the other.
 */
public enum EventSide {
    /**
     * Handler is only active on the logical client.
     */
    CLIENT,
    /**
     * Handler is only active on the logical server.
     */
    SERVER,
    /**
     * Handler is active on both client and server.
     */
    BOTH
}

