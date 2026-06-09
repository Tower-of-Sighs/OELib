package cc.sighs.oelib.config.optics;

import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents an internal zero-or-one value for the optics layer.
 *
 * <p>This type preserves presence and absence semantics for optic
 * composition. Public configuration APIs expose {@link Optional} instead of
 * this type.
 *
 * @param <A> the contained value type
 */
@ApiStatus.Internal
public sealed interface Maybe<A> permits Maybe.Some, Maybe.None {
    /**
     * Returns a present {@code Maybe} containing the given value.
     *
     * @param  value the contained value
     * @param  <A> the value type
     * @return a present maybe containing {@code value}
     */
    static <A> Maybe<A> some(A value) {
        return new Some<>(value);
    }

    /**
     * Returns the empty {@code Maybe}.
     *
     * @param  <A> the value type
     * @return the empty maybe
     */
    @SuppressWarnings("unchecked")
    static <A> Maybe<A> none() {
        return (Maybe<A>) None.INSTANCE;
    }

    /**
     * Returns a {@code Maybe} representing the same presence state as the
     * given {@link Optional}.
     *
     * @param  optional the optional to convert
     * @param  <A> the value type
     * @return {@link Some} when {@code optional} is present; {@link None}
     *         otherwise
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <A> Maybe<A> fromOptional(Optional<? extends A> optional) {
        Objects.requireNonNull(optional);
        return optional.<Maybe<A>>map(Maybe::some).orElseGet(Maybe::none);
    }

    /**
     * Tests whether this value is present.
     *
     * @return {@code true} if this value is present; {@code false} otherwise
     */
    boolean isPresent();

    /**
     * Tests whether this value is absent.
     *
     * @return {@code true} if this value is absent; {@code false} otherwise
     */
    default boolean isEmpty() {
        return !isPresent();
    }

    /**
     * Applies the given mapper to the contained value when present.
     *
     * <p>If this value is absent, the result is {@link #none()}.
     *
     * @param  mapper the mapping function
     * @param  <B> the mapped value type
     * @return a {@code Maybe} containing the mapped value, or {@link #none()}
     *         if this value is absent
     */
    <B> Maybe<B> map(Function<? super A, ? extends B> mapper);

    /**
     * Applies the given mapper to the contained value when present and returns
     * the resulting {@code Maybe}.
     *
     * <p>If this value is absent, the result is {@link #none()}.
     *
     * @param  mapper the mapping function
     * @param  <B> the mapped value type
     * @return the {@code Maybe} returned by {@code mapper}, or {@link #none()}
     *         if this value is absent
     */
    <B> Maybe<B> flatMap(Function<? super A, ? extends Maybe<? extends B>> mapper);

    /**
     * Applies one of the given branches according to whether this value is
     * absent or present.
     *
     * @param  onNone the branch used when this value is absent
     * @param  onSome the branch used when this value is present
     * @param  <R> the result type
     * @return the folded result
     */
    <R> R fold(Supplier<? extends R> onNone, Function<? super A, ? extends R> onSome);

    /**
     * Returns the contained value if present; otherwise returns the given
     * fallback.
     *
     * @param  fallback the fallback value
     * @return the contained value if present; {@code fallback} otherwise
     */
    A orElse(A fallback);

    /**
     * Returns the contained value if present; otherwise returns a value
     * produced by the given supplier.
     *
     * @param  fallback the fallback supplier
     * @return the contained value if present; otherwise the supplied fallback
     */
    A orElseGet(Supplier<? extends A> fallback);

    /**
     * Returns an {@link Optional} representing the same presence state as this
     * value.
     *
     * @return an {@code Optional} containing the present value, or
     *         {@code Optional.empty()} if this value is absent
     */
    Optional<A> toOptional();

    /**
     * Returns an {@link Either} whose right branch contains the present value
     * and whose left branch contains a fallback produced when this value is
     * absent.
     *
     * @param  leftSupplier the supplier for the left branch
     * @param  <L> the left type
     * @return {@code Either.right(value)} if this value is present;
     *         {@code Either.left(leftSupplier.get())} otherwise
     */
    <L> Either<L, A> toEither(Supplier<? extends L> leftSupplier);

    /**
     * Represents the present branch of {@link Maybe}.
     *
     * @param <A> the contained value type
     */
    record Some<A>(A value) implements Maybe<A> {
        public Some {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public <B> Maybe<B> map(Function<? super A, ? extends B> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return Maybe.some(mapper.apply(value));
        }

        @Override
        public <B> Maybe<B> flatMap(Function<? super A, ? extends Maybe<? extends B>> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return narrow(Objects.requireNonNull(mapper.apply(value), "mapper result"));
        }

        @Override
        public <R> R fold(Supplier<? extends R> onNone, Function<? super A, ? extends R> onSome) {
            Objects.requireNonNull(onNone, "onNone");
            Objects.requireNonNull(onSome, "onSome");
            return onSome.apply(value);
        }

        @Override
        public A orElse(A fallback) {
            return value;
        }

        @Override
        public A orElseGet(Supplier<? extends A> fallback) {
            Objects.requireNonNull(fallback, "fallback");
            return value;
        }

        @Override
        public Optional<A> toOptional() {
            return Optional.of(value);
        }

        @Override
        public <L> Either<L, A> toEither(Supplier<? extends L> leftSupplier) {
            Objects.requireNonNull(leftSupplier, "leftSupplier");
            return Either.right(value);
        }
    }

    /**
     * Represents the absent branch of {@link Maybe}.
     *
     * @param <A> the contained value type
     */
    final class None<A> implements Maybe<A> {
        private static final None<?> INSTANCE = new None<>();

        private None() {
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public <B> Maybe<B> map(Function<? super A, ? extends B> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return Maybe.none();
        }

        @Override
        public <B> Maybe<B> flatMap(Function<? super A, ? extends Maybe<? extends B>> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return Maybe.none();
        }

        @Override
        public <R> R fold(Supplier<? extends R> onNone, Function<? super A, ? extends R> onSome) {
            Objects.requireNonNull(onNone, "onNone");
            Objects.requireNonNull(onSome, "onSome");
            return onNone.get();
        }

        @Override
        public A orElse(A fallback) {
            return fallback;
        }

        @Override
        public A orElseGet(Supplier<? extends A> fallback) {
            Objects.requireNonNull(fallback, "fallback");
            return fallback.get();
        }

        @Override
        public Optional<A> toOptional() {
            return Optional.empty();
        }

        @Override
        public <L> Either<L, A> toEither(Supplier<? extends L> leftSupplier) {
            Objects.requireNonNull(leftSupplier, "leftSupplier");
            return Either.left(leftSupplier.get());
        }
    }

    @SuppressWarnings("unchecked")
    private static <A> Maybe<A> narrow(Maybe<? extends A> maybe) {
        return (Maybe<A>) maybe;
    }
}
