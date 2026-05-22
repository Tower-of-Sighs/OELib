package cc.sighs.oelib.config.optics;

import com.mojang.datafixers.util.Either;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A conditional accessor that may or may not match a value within a
 * configuration record.
 *
 * <p>Unlike a {@link ConfigLens}, which always succeeds, a prism has a
 * {@link #preview(Object)} operation that returns {@link Optional#empty()}
 * when the target does not match (for example, when an optional field is
 * absent, or a value is not of the expected subtype).
 *
 * <p>Prisms are created through {@link cc.sighs.oelib.config.RecordLensBuilder#optional(ConfigLens)
 * RecordLensBuilder.optional} and
 * {@link cc.sighs.oelib.config.RecordLensBuilder#subtype(ConfigLens, Class)
 * RecordLensBuilder.subtype}.
 *
 * @param <S> the source (record) type
 * @param <A> the target (matched) type
 */
public final class ConfigPrism<S, A> {
    private final String path;
    private final Function<S, Optional<A>> preview;
    private final BiFunction<A, S, S> setter;

    /**
     * Constructs a prism.
     *
     * @param path    the dotted path to the target field
     * @param preview a function that returns the matched value, or empty if no match
     * @param setter  a function that replaces the matched value in the source
     * @throws NullPointerException if any argument is {@code null}
     */
    public ConfigPrism(String path, Function<S, Optional<A>> preview, BiFunction<A, S, S> setter) {
        this.path = Objects.requireNonNull(path);
        this.preview = Objects.requireNonNull(preview);
        this.setter = Objects.requireNonNull(setter);
    }

    /**
     * Returns the dotted path to this target.
     *
     * @return the path
     */
    public String path() {
        return path;
    }

    /**
     * Attempts to match against the given source, returning either the
     * matched value on the right or the unmodified source on the left.
     *
     * @param source the source record
     * @return an {@link Either} with the matched value or the original source
     */
    public Either<S, A> match(S source) {
        Optional<A> matched = preview(source);
        return matched.<Either<S, A>>map(Either::right).orElseGet(() -> Either.left(source));
    }

    /**
     * Returns the matched value, or {@link Optional#empty()} if this prism
     * does not match.
     *
     * @param source the source record
     * @return the matched value, or empty
     */
    public Optional<A> preview(S source) {
        return preview.apply(source);
    }

    /**
     * Replaces the matched value in the source. If the prism does not match,
     * the behavior is undefined (the setter may still write).
     *
     * @param source the source record
     * @param value  the new value
     * @return a new record with the value replaced
     */
    public S set(S source, A value) {
        return setter.apply(value, source);
    }

    /**
     * Transforms the matched value if this prism matches, otherwise returns
     * the source unchanged.
     *
     * @param source  the source record
     * @param updater a function transforming the matched value
     * @return a new record with the value updated, or the original source
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public S updateIfPresent(S source, UnaryOperator<A> updater) {
        Objects.requireNonNull(updater);
        Optional<A> matched = preview(source);
        if (matched.isEmpty()) {
            return source;
        }
        return set(source, updater.apply(matched.get()));
    }
}
