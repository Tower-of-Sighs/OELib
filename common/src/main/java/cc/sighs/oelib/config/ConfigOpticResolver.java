package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.*;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves optics (lens, affine, traversal, fold) from record component
 * accessor method references, bound to a specific record class and
 * {@link MethodHandles.Lookup}.
 */

@ApiStatus.Internal
final class ConfigOpticResolver<T> {
    private final MethodHandles.Lookup lookup;
    private final Class<T> rootClass;

    ConfigOpticResolver(MethodHandles.Lookup lookup, Class<T> rootClass) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
        this.rootClass = Objects.requireNonNull(rootClass, "rootClass");
    }

    <A> ConfigLens<T, A> lens(RecordLensBuilder.LensGetter<T, A> getter) {
        return RecordLensBuilder.lens(lookup, rootClass, getter);
    }

    <A> ConfigAffine<T, A> optional(RecordLensBuilder.LensGetter<T, Optional<A>> getter) {
        return RecordLensBuilder.optional(lens(getter));
    }

    <A, X extends A> ConfigAffine<T, X> subtype(RecordLensBuilder.LensGetter<T, A> getter, Class<X> subtypeClass) {
        return RecordLensBuilder.subtype(lens(getter), subtypeClass);
    }

    <E> ConfigTraversal<T, E> listTraversal(RecordLensBuilder.LensGetter<T, List<E>> getter) {
        return Traversals.onList(lens(getter));
    }

    <K, V> ConfigTraversal<T, V> mapValuesTraversal(RecordLensBuilder.LensGetter<T, Map<K, V>> getter) {
        return Traversals.onMapValues(lens(getter));
    }

    <K, V> ConfigFold<T, K> mapKeysFold(RecordLensBuilder.LensGetter<T, Map<K, V>> getter) {
        return Folds.onMapKeys(lens(getter));
    }
}
