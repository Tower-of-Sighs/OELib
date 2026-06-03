package cc.sighs.oelib.config.optics.internal;

import cc.sighs.oelib.config.optics.ConfigLens;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Factory methods for creating {@link ConfigFold} instances over common
 * collection types.
 *
 * <p>This class complements {@link Traversals} and
 * {@link cc.sighs.oelib.config.ConfigUnit ConfigUnit}'s semantic API.
 */
@ApiStatus.Internal
public final class Folds {
    private Folds() {
    }

    /**
     * Creates a fold over the keys of a {@code Map} field addressed by the
     * given lens.
     *
     * @param lens a lens targeting a {@code Map<K, V>} field
     * @param <S>  the source type
     * @param <K>  the map key type
     * @param <V>  the map value type
     * @return a fold over the map keys
     */
    public static <S, K, V> ConfigFold<S, K> onMapKeys(ConfigLens<S, Map<K, V>> lens) {
        Objects.requireNonNull(lens);
        return new ConfigFold<>(
                lens.path(),
                source -> List.copyOf(lens.view(source).keySet())
        );
    }
}
