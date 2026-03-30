package cc.sighs.oelib.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler for the {@link EventBus}.
 * <p>
 * The annotated method must:
 * <ul>
 *     <li>be {@code void}-returning,</li>
 *     <li>have exactly one parameter implementing {@link Event},</li>
 *     <li>be either static or belong to an instance that is registered via {@link EventBus#register(Object)}.</li>
 * </ul>
 * Handlers are ordered by {@link #phase()} and {@link #priority()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
    EventPriority priority() default EventPriority.NORMAL;

    EventSide side() default EventSide.BOTH;

    boolean receiveCanceled() default false;
}
