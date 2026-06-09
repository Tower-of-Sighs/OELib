package cc.sighs.oelib.config;

import org.jetbrains.annotations.ApiStatus;

/**
 * A {@link ConfigMutation} that requires an {@link ConfigOpticResolver} to
 * resolve record-component accessors.
 *
 * <p>Instances are produced by the factory methods in {@link ConfigMutation}.
 * They are recognised by {@link ConfigUnit#updateAll} and
 * {@link ConfigUnitOps#updateAllNoSave} so that the resolver is injected
 * from the calling context.
 *
 * <p>Calling {@link #apply(Object)} on a {@code ContextualMutation} without
 * a resolver throws {@link UnsupportedOperationException}.
 *
 * @param <S> the configuration value type
 */
@ApiStatus.Internal
@FunctionalInterface
interface ContextualMutation<S> extends ConfigMutation<S> {
    /**
     * Applies this mutation using the given optic resolver.
     *
     * @param source   the configuration value to transform
     * @param resolver the resolver bound to the source record class
     * @return the transformed configuration value
     */
    S apply(S source, ConfigOpticResolver<S> resolver);

    @Override
    default S apply(S source) {
        throw new UnsupportedOperationException(
                "This mutation requires an explicit ConfigOpticResolver. " +
                        "Use ConfigUnit.updateAll() or ConfigUnitOps.updateAllNoSave() " +
                        "to execute built-in mutations."
        );
    }
}
