package cc.sighs.oelib.event;

/**
 * Result status for events with tri-state outcomes.
 * <p>
 * Use {@link Result#DENY} to forcefully disallow,
 * {@link Result#ALLOW} to forcefully allow,
 * and {@link Result#DEFAULT} to leave behavior unchanged.
 */
public enum Result {
    DENY,
    DEFAULT,
    ALLOW
}
