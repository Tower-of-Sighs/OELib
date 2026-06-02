package cc.sighs.oelib.config.optics.internal;

import cc.sighs.oelib.config.optics.ConfigLens;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Factory methods for creating {@link ConfigTraversal} instances over common
 * collection types.
 *
 * <p>This class complements {@link Folds} and
 * {@link cc.sighs.oelib.config.ConfigUnit ConfigUnit}'s semantic API.
 */
@ApiStatus.Internal
public final class Traversals {
    private Traversals() {
    }

    /**
     * Creates a traversal over the elements of a {@code List} field addressed
     * by the given lens.
     *
     * @param lens a lens targeting a {@code List<T>} field
     * @param <S>  the source type
     * @param <T>  the list element type
     * @return a traversal over the list elements
     */
    public static <S, T> ConfigTraversal<S, T> onList(ConfigLens<S, List<T>> lens) {
        Objects.requireNonNull(lens);
        return new ConfigTraversal<>(
                lens.path(),
                source -> List.copyOf(lens.view(source)),
                (source, op) -> lens.set(source, lens.view(source).stream().map(op).collect(Collectors.toList()))
        );
    }

    /**
     * Creates a traversal over the values of a {@code Map} field addressed by
     * the given lens.
     *
     * @param lens a lens targeting a {@code Map<K, V>} field
     * @param <S>  the source type
     * @param <K>  the map key type
     * @param <V>  the map value type
     * @return a traversal over the map values
     */
    public static <S, K, V> ConfigTraversal<S, V> onMapValues(ConfigLens<S, Map<K, V>> lens) {
        Objects.requireNonNull(lens);
        return new ConfigTraversal<>(
                lens.path(),
                source -> List.copyOf(lens.view(source).values()),
                (source, op) -> {
                    Map<K, V> map = lens.view(source);
                    Map<K, V> result = new LinkedHashMap<>(map.size());
                    for (Map.Entry<K, V> entry : map.entrySet()) {
                        result.put(entry.getKey(), op.apply(entry.getValue()));
                    }
                    return lens.set(source, result);
                }
        );
    }
}
