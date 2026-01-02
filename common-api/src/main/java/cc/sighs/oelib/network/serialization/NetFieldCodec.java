package cc.sighs.oelib.network.serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a custom {@code StreamCodec} to use for a record component.
 * <p>
 * The codec is obtained reflectively from a static field on the given holder
 * type. A typical usage looks like:
 * </p>
 * <pre>{@code
 * @NetFieldCodec(holder = BlockPos.class, field = "STREAM_CODEC")
 * BlockPos pos
 * }</pre>
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetFieldCodec {

    /**
     * Type that holds the static codec field.
     *
     * @return holder type
     */
    Class<?> holder();

    /**
     * Name of the static codec field.
     *
     * @return field name, defaults to {@code "STREAM_CODEC"}
     */
    String field();
}

