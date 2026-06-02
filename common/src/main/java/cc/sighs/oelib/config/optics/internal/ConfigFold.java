package cc.sighs.oelib.config.optics.internal;

import cc.sighs.oelib.config.optics.ConfigLens;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A read-only multi-focus optic that collects zero or more values of type
 * {@code A} from a source {@code S}.
 *
 * <p>{@code ConfigFold} supports query operations ({@link #extract},
 * {@link #count}, {@link #anyMatch}, and {@link #allMatch}) and composition
 * with {@link ConfigLens lenses} and traversals.
 *
 * @param <S> the source type
 * @param <A> the element type
 */
@ApiStatus.Internal
public class ConfigFold<S, A> {
    private final String path;
    private final Function<S, List<A>> extractFn;

    /**
     * Constructs a fold with the specified path and extraction function.
     *
     * @param path      the path to the focused elements
     * @param extractFn a function that extracts all focused values from the source
     */
    public ConfigFold(String path, Function<S, List<A>> extractFn) {
        this.path = Objects.requireNonNull(path);
        this.extractFn = Objects.requireNonNull(extractFn);
    }

    /**
     * Returns the path to the focused elements.
     *
     * @return the path
     */
    public String path() {
        return path;
    }

    /**
     * Extracts all focused values from the given source.
     *
     * @param source the source value
     * @return an immutable list of focused values
     */
    public List<A> extract(S source) {
        return extractFn.apply(source);
    }

    /**
     * Combines the focused values into a single result using the supplied
     * accumulator and identity value.
     *
     * @param source      the source value
     * @param identity    the identity value for the fold
     * @param accumulator a function that combines the accumulated result with
     *                    each focused value
     * @param <R>         the result type
     * @return the accumulated result
     */
    public <R> R fold(S source, R identity, BiFunction<R, ? super A, R> accumulator) {
        R result = identity;
        for (A a : extract(source)) {
            result = accumulator.apply(result, a);
        }
        return result;
    }

    /**
     * Returns the first focused value that matches the given predicate.
     *
     * @param source    the source value
     * @param predicate a predicate to test against focused values
     * @return the first matching value, or {@link Optional#empty()} if none
     *         matches
     */
    public Optional<A> findFirst(S source, Predicate<? super A> predicate) {
        for (A a : extract(source)) {
            if (predicate.test(a)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns {@code true} if any focused value satisfies the given predicate.
     *
     * @param source    the source value
     * @param predicate a predicate to test against focused values
     * @return {@code true} if any element satisfies the predicate
     */
    public boolean anyMatch(S source, Predicate<? super A> predicate) {
        return extract(source).stream().anyMatch(predicate);
    }

    /**
     * Returns {@code true} if every focused value satisfies the given
     * predicate, or if there are no focused values.
     *
     * @param source    the source value
     * @param predicate a predicate to test against focused values
     * @return {@code true} if every element satisfies the predicate, or
     *         {@code true} if no elements exist
     */
    public boolean allMatch(S source, Predicate<? super A> predicate) {
        return extract(source).stream().allMatch(predicate);
    }

    /**
     * Returns the number of focused values in the given source.
     *
     * @param source the source value
     * @return the count of focused values
     */
    public long count(S source) {
        return extract(source).size();
    }

    /**
     * Creates a new fold that includes only values matching the given
     * predicate.
     *
     * @param predicate a predicate used to filter focused values
     * @return a filtered fold
     */
    public ConfigFold<S, A> filter(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate);
        return new ConfigFold<>(
                path,
                source -> extract(source).stream().filter(predicate).collect(Collectors.toList())
        );
    }

    /**
     * Composes this fold with a lens, producing a fold that focuses on the
     * lens target within each element.
     *
     * @param lens the lens to apply to each focused value
     * @param <B>  the target type of the lens
     * @return a composed fold
     */
    public <B> ConfigFold<S, B> compose(ConfigLens<A, B> lens) {
        Objects.requireNonNull(lens);
        return new ConfigFold<>(
                path + "." + lens.path(),
                source -> {
                    List<A> as = extract(source);
                    List<B> bs = new ArrayList<>(as.size());
                    for (A a : as) {
                        bs.add(lens.view(a));
                    }
                    return bs;
                }
        );
    }

    /**
     * Composes this fold with a traversal, producing a fold that focuses on
     * the traversal elements within each element of this fold.
     *
     * @param traversal the traversal to apply to each focused value
     * @param <B>       the element type of the traversal
     * @return a composed fold
     */
    public <B> ConfigFold<S, B> compose(ConfigTraversal<A, B> traversal) {
        Objects.requireNonNull(traversal);
        return new ConfigFold<>(
                path + "." + traversal.path(),
                source -> {
                    List<A> as = extract(source);
                    List<B> result = new ArrayList<>();
                    for (A a : as) {
                        result.addAll(traversal.extract(a));
                    }
                    return result;
                }
        );
    }
}
