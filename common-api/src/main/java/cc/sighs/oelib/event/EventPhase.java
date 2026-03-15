package cc.sighs.oelib.event;

/**
 * Phase of an event handler.
 * <p>
 * There are two typical ways to use phases:
 * <ul>
 *   <li><b>Ordering within a single dispatch</b>: {@code PRE -> NORMAL -> POST} ordering is applied
 *   when you call {@link EventBus#post(Event)}; all phases will run in order.</li>
 *   <li><b>Exact placement around native handling</b>: platform bridges can call
 *   {@link EventBus#postPhase(Event, EventPhase)} before and after the native routine to emulate
 *   "Pre"/"Post" style hooks (e.g., input handling, screen lifecycle, etc.).</li>
 * </ul>
 * For strict "Pre/Post" semantics tied to a native action, prefer {@link EventBus#postPhase(Event, EventPhase)}
 * (or define dedicated {@code MyEvent.Pre}/{@code MyEvent.Post} types) instead of relying solely on ordering.
 */
public enum EventPhase {
    /**
     * Run before the main/native handling, or first in a single dispatch.
     */
    PRE,
    /**
     * The main phase for the current hook.
     */
    NORMAL,
    /**
     * Run after the main/native handling, or last in a single dispatch.
     */
    POST
}
