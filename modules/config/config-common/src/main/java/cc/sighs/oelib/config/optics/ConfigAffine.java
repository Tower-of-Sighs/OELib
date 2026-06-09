package cc.sighs.oelib.config.optics;

import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Conditional optic over at most one value of type {@code A} within a source
 * of type {@code S}.
 *
 * <p>This optic exposes its query result as {@link Optional} for public API
 * compatibility. Internally, implementations may use a different zero-or-one
 * representation while preserving the contracts of {@link #preview(Object)},
 * {@link #match(Object)}, and {@link #updateIfPresent(Object, UnaryOperator)}.
 *
 * @param <S> the source type
 * @param <A> the focused value type
 *
 * @see cc.sighs.oelib.config.RecordLensBuilder#optional(ConfigLens)
 * @see cc.sighs.oelib.config.RecordLensBuilder#subtype(ConfigLens, Class)
 */
@ApiStatus.Internal
public final class ConfigAffine<S, A> {
    private final String path;
    private final Function<S, Maybe<A>> select;
    private final BiFunction<A, S, S> setter;

    /**
     * Constructs an affine with the given path, preview function, and setter.
     *
     * @param  path the dotted path identifying this focus
     * @param  preview the function that returns the matched value when present,
     *         or {@code Optional.empty()} when no value is focused
     * @param  setter the function that replaces the matched value in the source
     */
    public ConfigAffine(String path, Function<S, Optional<A>> preview, BiFunction<A, S, S> setter) {
        this.path = Objects.requireNonNull(path);
        Objects.requireNonNull(preview);
        this.select = source -> Maybe.fromOptional(preview.apply(source));
        this.setter = Objects.requireNonNull(setter);
    }

    /**
     * Returns the dotted path identifying this focus.
     *
     * @return the dotted path
     */
    public String path() {
        return path;
    }

    /**
     * Returns either the focused value or the original source when no value is
     * focused.
     *
     * @param  source the source value
     * @return {@code Either.right(value)} when this affine focuses a value
     *         within {@code source}; {@code Either.left(source)} otherwise
     */
    public Either<S, A> match(S source) {
        return select.apply(source).toEither(() -> source);
    }

    /**
     * Returns the focused value, if any.
     *
     * @param  source the source value
     * @return an {@code Optional} containing the focused value, or
     *         {@code Optional.empty()} if no value is focused
     */
    public Optional<A> preview(S source) {
        return select.apply(source).toOptional();
    }

    /**
     * Replaces the focused value in the given source.
     *
     * <p>If this affine does not focus a value within {@code source}, the
     * result is unspecified.
     *
     * @param  source the source value
     * @param  value the replacement value
     * @return a source value with the focused value replaced
     */
    public S set(S source, A value) {
        return setter.apply(value, source);
    }

    /**
     * Applies the given updater to the focused value when present.
     *
     * @param  source the source value
     * @param  updater the function that transforms the focused value
     * @return a source value with the focused value updated, or {@code source}
     *         if no value is focused
     */
    public S updateIfPresent(S source, UnaryOperator<A> updater) {
        Objects.requireNonNull(updater);
        return select.apply(source).fold(
                () -> source,
                value -> set(source, updater.apply(value))
        );
    }
}
