package cc.sighs.oelib.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Performs path-based operations for a {@link ConfigUnit}.
 *
 * <p>This type accepts preconstructed {@link ConfigPath} instances. Methods
 * operate on the path cardinality rather than on the field type that produced
 * the path.
 *
 * @param <T> the configuration value type
 */
public final class ConfigUnitPaths<T> {
    private final ConfigUnit<T> unit;

    ConfigUnitPaths(ConfigUnit<T> unit) {
        this.unit = Objects.requireNonNull(unit);
    }

    /**
     * Reads the value selected by an exactly-one path.
     *
     * @param  path the path selecting exactly one value
     * @param  <V> the focused value type
     * @return the focused value
     */
    public <V> V view(ConfigPath.One<T, V> path) {
        Objects.requireNonNull(path);
        return path.view(unit.get());
    }

    /**
     * Returns the value selected by a zero-or-one path, if any.
     *
     * @param  path the path selecting zero or one value
     * @param  <V> the focused value type
     * @return an {@link Optional} containing the focused value, or
     *         {@link Optional#empty()} if no value is focused
     */
    public <V> Optional<V> preview(ConfigPath.Maybe<T, V> path) {
        Objects.requireNonNull(path);
        return path.preview(unit.get());
    }

    /**
     * Returns the value selected by an exactly-one path when it has the
     * specified runtime type.
     *
     * @param  path the path selecting exactly one value
     * @param  subtype the expected subtype class
     * @param  <V> the base focused value type
     * @param  <X> the subtype to match
     * @return an {@link Optional} containing the matched value, or
     *         {@link Optional#empty()} if the value has a different type
     */
    public <V, X extends V> Optional<X> preview(ConfigPath.One<T, V> path, Class<X> subtype) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(subtype);
        V value = path.view(unit.get());
        return subtype.isInstance(value) ? Optional.of(subtype.cast(value)) : Optional.empty();
    }

    /**
     * Updates the value selected by an exactly-one path.
     *
     * @param  path the path selecting exactly one value
     * @param  updater the function that transforms the focused value
     * @param  <V> the focused value type
     * @return the committed configuration value
     */
    public <V> T update(ConfigPath.One<T, V> path, UnaryOperator<V> updater) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(updater);
        T current = unit.get();
        T updated = path.update(current, updater);
        return unit.commitCandidate(current, updated, true);
    }

    /**
     * Updates the value selected by a zero-or-one path when it is present.
     *
     * @param  path the path selecting zero or one value
     * @param  updater the function that transforms the focused value
     * @param  <V> the focused value type
     * @return the committed configuration value
     */
    public <V> T ifPresent(ConfigPath.Maybe<T, V> path, UnaryOperator<V> updater) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(updater);
        T current = unit.get();
        T updated = path.updateIfPresent(current, updater);
        return unit.commitCandidate(current, updated, true);
    }

    /**
     * Updates the value selected by an exactly-one path when it has the
     * specified runtime type.
     *
     * @param  path the path selecting exactly one value
     * @param  subtype the expected subtype class
     * @param  updater the function that transforms the matched value
     * @param  <V> the base focused value type
     * @param  <X> the subtype to match
     * @return the committed configuration value
     */
    public <V, X extends V> T whenSubtype(ConfigPath.One<T, V> path, Class<X> subtype, UnaryOperator<X> updater) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(subtype);
        Objects.requireNonNull(updater);
        T current = unit.get();
        V focused = path.view(current);
        T updated = subtype.isInstance(focused)
                ? path.set(current, updater.apply(subtype.cast(focused)))
                : current;
        return unit.commitCandidate(current, updated, true);
    }

    /**
     * Updates all values selected by a zero-or-more path.
     *
     * @param  path the path selecting zero or more values
     * @param  updater the function that transforms each focused value
     * @param  <V> the focused value type
     * @return the committed configuration value
     */
    public <V> T updateEach(ConfigPath.Many<T, V> path, UnaryOperator<V> updater) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(updater);
        T current = unit.get();
        T updated = path.updateEach(current, updater);
        return unit.commitCandidate(current, updated, true);
    }

    /**
     * Updates values selected by a zero-or-more path that satisfy the given
     * predicate.
     *
     * @param  path the path selecting zero or more values
     * @param  predicate the predicate that selects values to update
     * @param  updater the function that transforms each selected value
     * @param  <V> the focused value type
     * @return the committed configuration value
     */
    public <V> T updateWhere(ConfigPath.Many<T, V> path, Predicate<? super V> predicate, UnaryOperator<V> updater) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(updater);
        T current = unit.get();
        T updated = path.where(predicate).updateEach(current, updater);
        return unit.commitCandidate(current, updated, true);
    }

    /**
     * Returns all values selected by a zero-or-more path.
     *
     * @param  path the path selecting zero or more values
     * @param  <V> the focused value type
     * @return an immutable list of focused values
     */
    public <V> List<V> getAll(ConfigPath.Many<T, V> path) {
        Objects.requireNonNull(path);
        return path.getAll(unit.get());
    }

    /**
     * Returns selected values that satisfy the given predicate.
     *
     * @param  path the path selecting zero or more values
     * @param  predicate the predicate that selects values to return
     * @param  <V> the focused value type
     * @return an immutable list of matching focused values
     */
    public <V> List<V> getAllWhere(ConfigPath.Many<T, V> path, Predicate<? super V> predicate) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(predicate);
        return path.where(predicate).getAll(unit.get());
    }

    /**
     * Returns the number of values selected by a zero-or-more path.
     *
     * @param  path the path selecting zero or more values
     * @param  <V> the focused value type
     * @return the number of focused values
     */
    public <V> long count(ConfigPath.Many<T, V> path) {
        Objects.requireNonNull(path);
        return path.count(unit.get());
    }

    /**
     * Tests whether any selected value satisfies the given predicate.
     *
     * @param  path the path selecting zero or more values
     * @param  predicate the predicate to test
     * @param  <V> the focused value type
     * @return {@code true} if any focused value satisfies the predicate
     */
    public <V> boolean anyMatch(ConfigPath.Many<T, V> path, Predicate<? super V> predicate) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(predicate);
        return path.anyMatch(unit.get(), predicate);
    }

    /**
     * Tests whether all selected values satisfy the given predicate.
     *
     * @param  path the path selecting zero or more values
     * @param  predicate the predicate to test
     * @param  <V> the focused value type
     * @return {@code true} if all focused values satisfy the predicate, or
     *         if no values are focused
     */
    public <V> boolean allMatch(ConfigPath.Many<T, V> path, Predicate<? super V> predicate) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(predicate);
        return path.allMatch(unit.get(), predicate);
    }

    /**
     * Returns the first selected value that satisfies the given predicate.
     *
     * @param  path the path selecting zero or more values
     * @param  predicate the predicate to test
     * @param  <V> the focused value type
     * @return an {@link Optional} containing the first matching focused value,
     *         or {@link Optional#empty()} if no value matches
     */
    public <V> Optional<V> findFirst(ConfigPath.Many<T, V> path, Predicate<? super V> predicate) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(predicate);
        return path.findFirst(unit.get(), predicate);
    }
}
