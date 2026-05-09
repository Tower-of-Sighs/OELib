package cc.sighs.oelib.config;

/**
 * A pure function that transforms a configuration value of type {@code S}
 * into a new value of the same type.
 *
 * <p>Mutations are intended to be composed and applied in batch via
 * {@link ConfigUnit#updateAll(ConfigMutation[])} or
 * {@link ConfigUnitOps#updateAllNoSave(ConfigUnit, ConfigMutation[])}.
 * They can be created directly from a lambda or via factory methods on
 * {@link cc.sighs.oelib.config.optics.ConfigLens ConfigLens} such as
 * {@code setTo} and {@code map}.
 *
 * @param <S> the type of the configuration value
 */
@FunctionalInterface
public interface ConfigMutation<S> {
    /**
     * Applies this mutation to the given source value.
     *
     * @param source the current configuration value
     * @return the transformed configuration value
     */
    S apply(S source);
}
