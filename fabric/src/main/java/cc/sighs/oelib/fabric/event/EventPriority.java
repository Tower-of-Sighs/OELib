package cc.sighs.oelib.fabric.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventPriority {

    int HIGHEST = -1000;

    int VERY_HIGH = -750;

    int HIGH = -500;

    int ABOVE_NORMAL = -250;

    int NORMAL = 0;

    int BELOW_NORMAL = 250;

    int LOW = 500;

    int VERY_LOW = 750;

    int LOWEST = 1000;

    int priority() default NORMAL;

    String description() default "";
}