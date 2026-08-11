package cc.sighs.oelib.config;

import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.util.OptionalOps;
import com.flechazo.optics.Affine;
import com.flechazo.optics.Lens;
import com.flechazo.optics.LensGetter;
import com.flechazo.optics.util.Prisms;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.*;
import java.util.*;

/**
 *  Creates record and container optics from serializable record component accessors.
 */
@ApiStatus.Internal
public final class RecordLensBuilder {
    private RecordLensBuilder() {
    }

    static <S, A> Lens<S, A> lens(Class<S> recordClass, LensGetter<S, A> getter) {
        Objects.requireNonNull(recordClass);
        Objects.requireNonNull(getter);
        return Lens.of(recordClass, getter);
    }

    static <S, A> Affine<S, A> optional(Lens<S, Optional<A>> lens) {
        Objects.requireNonNull(lens);
        Affine<Optional<A>, A> optionalValue = Affine.of(
                optional -> OptionalOps.toEither(optional, () -> optional),
                (optional, value) -> Optional.of(value)
        );
        return lens.andThen(optionalValue);
    }

    static <S, A, X extends A> Affine<S, X> subtype(Lens<S, A> lens, Class<X> subtypeClass) {
        Objects.requireNonNull(lens);
        Objects.requireNonNull(subtypeClass);
        return lens.andThen(Prisms.instanceOf(subtypeClass));
    }

    static String componentName(LensGetter<?, ?> getter) {
        return Try.of(() -> serializedLambda(getter).getImplMethodName()).fold(
                error -> { throw new IllegalStateException(
                        "Failed to inspect getter lambda; use a record accessor method reference", error); },
                name -> name);
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

    static Class<?> setElementType(LensGetter<?, ?> getter) {
        return genericReturnTypeArgument(getter, Set.class, 0);
    }

    static Class<?> arrayElementType(LensGetter<?, ?> getter) {
        Type returnType = genericReturnType(getter);
        if (returnType instanceof Class<?> arrayClass && arrayClass.isArray()) {
            return arrayClass.getComponentType();
        }
        if (returnType instanceof GenericArrayType arrayType
                && arrayType.getGenericComponentType() instanceof Class<?> componentClass) {
            return componentClass;
        }
        throw new IllegalArgumentException("Getter return type is not an array with a reifiable component type");
    }

    static Class<?> mapKeyType(LensGetter<?, ?> getter) {
        return genericReturnTypeArgument(getter, Map.class, 0);
    }

    static Class<?> mapValueType(LensGetter<?, ?> getter) {
        return genericReturnTypeArgument(getter, Map.class, 1);
    }

    private static Class<?> genericReturnTypeArgument(LensGetter<?, ?> getter, Class<?> rawType, int index) {
        Type returnType = genericReturnType(getter);
        if (returnType instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() == rawType) {
            Type argument = parameterizedType.getActualTypeArguments()[index];
            if (argument instanceof Class<?> clazz) {
                return clazz;
            }
        }
        throw new IllegalArgumentException(
                "Getter return type is not " + rawType.getSimpleName() + " with a reifiable type argument"
        );
    }

    private static Type genericReturnType(LensGetter<?, ?> getter) {
        return Try.of(() -> {
            SerializedLambda lambda = serializedLambda(getter);
            Class<?> implClass = Class.forName(
                    lambda.getImplClass().replace('/', '.'),
                    false,
                    getter.getClass().getClassLoader()
            );
            Method method = implClass.getDeclaredMethod(lambda.getImplMethodName());
            return method.getGenericReturnType();
        }).fold(
                error -> { throw new IllegalStateException(
                        "Failed to inspect getter lambda; use a record accessor method reference", error); },
                returnType -> returnType);
    }

    private static SerializedLambda serializedLambda(LensGetter<?, ?> getter) throws ReflectiveOperationException {
        try {
            MethodHandles.Lookup projectLookup = ConfigOpticsLookupProvider.registeredLookup(getter.getClass());
            MethodHandles.Lookup lambdaLookup = MethodHandles.privateLookupIn(getter.getClass(), projectLookup);
            Object replacement = lambdaLookup.findVirtual(
                    getter.getClass(),
                    "writeReplace",
                    MethodType.methodType(Object.class)
            ).invoke(getter);
            if (replacement instanceof SerializedLambda lambda) {
                return lambda;
            }
            throw new IllegalArgumentException("Getter did not serialize to SerializedLambda");
        } catch (Throwable e) {
            if (e instanceof ReflectiveOperationException reflective) {
                throw reflective;
            }
            throw new ReflectiveOperationException("Failed to resolve getter SerializedLambda", e);
        }
    }
}
