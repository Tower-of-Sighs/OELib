package cc.sighs.oelib.event;

/**
 * Marker for events that carry a tri-state {@link Result}.
 * <p>
 * Default result should be {@link Result#DEFAULT}.
 */
public interface ResultEvent extends Event {
    Result getResult();
    void setResult(Result result);
}