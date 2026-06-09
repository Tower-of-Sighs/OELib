package cc.sighs.oelib.config.optics;

import com.mojang.datafixers.util.Either;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MaybeTest {

    @Test
    void someMapsAndFlattens() {
        Maybe<Integer> maybe = Maybe.some(4);

        assertEquals(Optional.of(5), maybe.map(v -> v + 1).toOptional());
        assertEquals(Optional.of(8), maybe.flatMap(v -> Maybe.some(v * 2)).toOptional());
        assertEquals(12, maybe.fold(() -> 0, v -> v * 3));
        assertEquals(Either.right(4), maybe.toEither(() -> -1));
    }

    @Test
    void nonePreservesAbsenceAcrossOperations() {
        Maybe<Integer> maybe = Maybe.none();

        assertTrue(maybe.isEmpty());
        assertFalse(maybe.isPresent());
        assertEquals(Optional.empty(), maybe.map(v -> v + 1).toOptional());
        assertEquals(Optional.empty(), maybe.flatMap(v -> Maybe.some(v + 1)).toOptional());
        assertEquals(9, maybe.fold(() -> 9, v -> v * 3));
        assertEquals(Either.left("missing"), maybe.toEither(() -> "missing"));
        assertEquals(7, maybe.orElse(7));
    }

    @Test
    void fromOptionalBridgesBothDirections() {
        assertEquals(Optional.of(3), Maybe.fromOptional(Optional.of(3)).toOptional());
        assertEquals(Optional.empty(), Maybe.fromOptional(Optional.<Integer>empty()).toOptional());
    }

    @Test
    void someRejectsNullValues() {
        assertThrows(NullPointerException.class, () -> Maybe.some(null));
        assertThrows(NullPointerException.class, () -> Maybe.some(1).map(v -> null));
        assertThrows(NullPointerException.class, () -> Maybe.some(1).flatMap(v -> null));
    }
}
