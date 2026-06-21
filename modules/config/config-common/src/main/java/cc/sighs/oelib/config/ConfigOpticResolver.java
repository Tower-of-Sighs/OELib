package cc.sighs.oelib.config;

import com.flechazo.optics.*;
import com.flechazo.optics.generated.LensGetter;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves generated record accessors for configuration operations.
 */
@ApiStatus.Internal
final class ConfigOpticResolver<T> {
    private final MethodHandles.Lookup lookup;
    private final Class<T> rootClass;

    ConfigOpticResolver(MethodHandles.Lookup lookup, Class<T> rootClass) {
        this.lookup = Objects.requireNonNull(lookup, "lookup");
        this.rootClass = Objects.requireNonNull(rootClass, "rootClass");
    }

    <A> Lens<T, A> lens(LensGetter<T, A> getter) {
        return RecordLensBuilder.lens(lookup, rootClass, getter);
    }

    <A> Affine<T, A> optional(LensGetter<T, Optional<A>> getter) {
        return RecordLensBuilder.optional(lens(getter));
    }

    <A, X extends A> Affine<T, X> subtype(LensGetter<T, A> getter, Class<X> subtypeClass) {
        return RecordLensBuilder.subtype(lens(getter), subtypeClass);
    }

    <E> Traversal<T, E> listTraversal(LensGetter<T, List<E>> getter) {
        return lens(getter).andThen(Each.listTraversal());
    }

    <K, V> Traversal<T, V> mapValuesTraversal(LensGetter<T, Map<K, V>> getter) {
        return lens(getter).andThen(Traversal.mapValues());
    }

    <K, V> Fold<T, K> mapKeysFold(LensGetter<T, Map<K, V>> getter) {
        return lens(getter).andThen(Fold.mapKeys());
    }
}
