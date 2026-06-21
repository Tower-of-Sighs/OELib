package cc.sighs.oelib.config;

import com.flechazo.optics.Affine;
import com.flechazo.optics.Lens;
import com.flechazo.optics.generated.LensGetter;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.util.Affines;
import com.flechazo.optics.util.Prisms;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves generated accessors for record component getter references.
 */
public final class RecordLensBuilder {
    private static final MethodHandles.Lookup INTERNAL_LOOKUP = MethodHandles.lookup();

    private RecordLensBuilder() {
    }

    static <S, A> Lens<S, A> lens(Class<S> recordClass, LensGetter<S, A> getter) {
        return lens(INTERNAL_LOOKUP, recordClass, getter);
    }

    static <S, A> Lens<S, A> lens(MethodHandles.Lookup lookup, Class<S> recordClass, LensGetter<S, A> getter) {
        Objects.requireNonNull(lookup);
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(getter);
        return RecordOptics.recordLens(recordClass, getter, lookup);
    }

    static <S, A> Affine<S, A> optional(Lens<S, Optional<A>> lens) {
        Objects.requireNonNull(lens);
        return lens.andThen(Affines.optionalValue());
    }

    static <S, A, X extends A> Affine<S, X> subtype(Lens<S, A> lens, Class<X> subtypeClass) {
        Objects.requireNonNull(lens);
        Objects.requireNonNull(subtypeClass);
        return lens.andThen(Prisms.instanceOf(subtypeClass));
    }

    static String componentName(LensGetter<?, ?> getter) {
        try {
            return serializedLambda(getter).getImplMethodName();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to inspect getter lambda; use a record accessor method reference", e);
        }
    }

    static Class<?> componentType(Class<?> recordClass, String componentName) {
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(componentName);
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("Only record types are supported: " + recordClass.getName());
        }
        for (RecordComponent component : recordClass.getRecordComponents()) {
            if (component.getName().equals(componentName)) {
                return component.getType();
            }
        }
        throw new IllegalArgumentException("Unknown record component '" + componentName + "' in " + recordClass.getName());
    }

    static Class<?> optionalElementType(LensGetter<?, ?> getter) {
        return genericReturnTypeArgument(getter, Optional.class, 0);
    }

    static Class<?> listElementType(LensGetter<?, ?> getter) {
        return genericReturnTypeArgument(getter, List.class, 0);
    }

    static Class<?> mapKeyType(LensGetter<?, ?> getter) {
        return genericReturnTypeArgument(getter, Map.class, 0);
    }

    static Class<?> mapValueType(LensGetter<?, ?> getter) {
        return genericReturnTypeArgument(getter, Map.class, 1);
    }

    private static Class<?> genericReturnTypeArgument(LensGetter<?, ?> getter, Class<?> rawType, int index) {
        try {
            SerializedLambda lambda = serializedLambda(getter);
            Class<?> implClass = Class.forName(lambda.getImplClass().replace('/', '.'));
            Method method = implClass.getDeclaredMethod(lambda.getImplMethodName());
            Type returnType = method.getGenericReturnType();
            if (returnType instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() == rawType) {
                Type argument = parameterizedType.getActualTypeArguments()[index];
                if (argument instanceof Class<?> clazz) {
                    return clazz;
                }
            }
            throw new IllegalArgumentException(
                    "Getter return type is not " + rawType.getSimpleName() + " with a reifiable type argument: " + lambda.getImplMethodName()
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to inspect getter lambda; use a record accessor method reference", e);
        }
    }

    private static SerializedLambda serializedLambda(LensGetter<?, ?> getter) throws ReflectiveOperationException {
        Method method = getter.getClass().getDeclaredMethod("writeReplace");
        method.setAccessible(true);
        return (SerializedLambda) method.invoke(getter);
    }
}
