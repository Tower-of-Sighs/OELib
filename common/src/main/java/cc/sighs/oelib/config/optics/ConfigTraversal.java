package cc.sighs.oelib.config.optics;

import cc.sighs.oelib.config.ConfigMutation;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A read-write multi-focus optic that can view and update zero or more values
 * of type {@code A} within a source {@code S}.
 *
 * <p>{@code ConfigTraversal} extends the read operations of {@link ConfigFold}
 * with an {@link #update} operation and supports composition with
 * {@link ConfigLens lenses} and other traversals.
 *
 * @param <S> the source type
 * @param <A> the element type
 */
@ApiStatus.Internal
public final class ConfigTraversal<S, A> extends ConfigFold<S, A> {
    private final BiFunction<S, UnaryOperator<A>, S> updateAll;

    /**
     * Constructs a traversal with the specified path, extraction function, and
     * update function.
     *
     * @param path      the path to the focused elements
     * @param extractFn a function that extracts all focused values from the source
     * @param updateAll a function that applies a modifier to all focused values
     *                  within the source and returns a new source
     */
    public ConfigTraversal(String path, Function<S, List<A>> extractFn, BiFunction<S, UnaryOperator<A>, S> updateAll) {
        super(path, extractFn);
        this.updateAll = Objects.requireNonNull(updateAll);
    }

    /**
     * Applies the given modifier to every focused value and returns a new
     * source.
     *
     * @param source   the source value
     * @param modifier a function that transforms each focused value
     * @return a new source with all focused values transformed
     */
    public S update(S source, UnaryOperator<A> modifier) {
        Objects.requireNonNull(modifier);
        return updateAll.apply(source, modifier);
    }

    /**
     * Creates a new traversal that focuses only on values matching the given
     * predicate.
     *
     * <p>Values that do not match the predicate are excluded from
     * {@link #extract} results and are left unchanged by {@link #update}.
     *
     * @param predicate a predicate used to filter focused values
     * @return a filtered traversal
     */
    @Override
    public ConfigTraversal<S, A> filter(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate);
        return new ConfigTraversal<>(
                path(),
                source -> extract(source).stream().filter(predicate).collect(Collectors.toList()),
                (source, op) -> updateAll.apply(source, a -> predicate.test(a) ? op.apply(a) : a)
        );
    }

    /**
     * Composes this traversal with a lens, producing a traversal that focuses
     * on the lens target within each focused element.
     *
     * @param lens the lens to apply to each element
     * @param <B>  the target type of the lens
     * @return a composed traversal
     */
    public <B> ConfigTraversal<S, B> compose(ConfigLens<A, B> lens) {
        Objects.requireNonNull(lens);
        return new ConfigTraversal<>(
                path() + "." + lens.path(),
                source -> {
                    List<A> as = extract(source);
                    List<B> bs = new ArrayList<>(as.size());
                    for (A a : as) {
                        bs.add(lens.view(a));
                    }
                    return bs;
                },
                (source, op) -> updateAll.apply(source, a -> lens.set(a, op.apply(lens.view(a))))
        );
    }

    /**
     * Composes this traversal with another traversal, producing a traversal
     * that focuses on the inner traversal elements within each element of this
     * traversal.
     *
     * @param other the traversal to apply to each element
     * @param <B>   the element type of the inner traversal
     * @return a composed traversal
     */
    public <B> ConfigTraversal<S, B> compose(ConfigTraversal<A, B> other) {
        Objects.requireNonNull(other);
        return new ConfigTraversal<>(
                path() + "." + other.path(),
                source -> {
                    List<A> as = extract(source);
                    List<B> result = new ArrayList<>();
                    for (A a : as) {
                        result.addAll(other.extract(a));
                    }
                    return result;
                },
                (source, op) -> updateAll.apply(source, a -> other.update(a, op))
        );
    }

    /**
     * Returns a {@link ConfigMutation} that, when applied to a source value,
     * transforms all focused elements using the given modifier.
     *
     * @param modifier a function that transforms each focused value
     * @return a mutation that performs this traversal update
     */
    public ConfigMutation<S> toMutation(UnaryOperator<A> modifier) {
        Objects.requireNonNull(modifier);
        return source -> update(source, modifier);
    }
}
