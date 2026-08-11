package cc.sighs.oelib.config;

import com.flechazo.optics.Affine;
import com.flechazo.optics.Lens;
import com.mojang.datafixers.util.Either;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPrismOptionalTest {

    @Test
    void optionalPrismUpdatesOnlyWhenPresent() {
        Lens<OptionalRoot, Optional<Integer>> lens = RecordLensBuilder.lens(OptionalRoot.class, OptionalRoot::value);
        Affine<OptionalRoot, Integer> affine = RecordLensBuilder.optional(lens);

        OptionalRoot present = new OptionalRoot(Optional.of(5), 9);
        OptionalRoot absent = new OptionalRoot(Optional.empty(), 9);

        OptionalRoot presentUpdated = affine.modify(value -> value + 3, present);
        OptionalRoot absentUpdated = affine.modify(value -> value + 3, absent);

        assertEquals(Optional.of(8), presentUpdated.value());
        assertEquals(9, presentUpdated.marker());
        assertEquals(Optional.empty(), absentUpdated.value());
        assertEquals(9, absentUpdated.marker());
    }

    @Test
    void optionalPrismStressLoopKeepsCorrectValue() {
        Lens<OptionalRoot, Optional<Integer>> lens = RecordLensBuilder.lens(OptionalRoot.class, OptionalRoot::value);
        Affine<OptionalRoot, Integer> affine = RecordLensBuilder.optional(lens);

        OptionalRoot current = new OptionalRoot(Optional.of(1), 1);
        for (int i = 0; i < 50_000; i++) {
            current = affine.modify(value -> value + 1, current);
        }

        assertEquals(Optional.of(50_001), current.value());
    }

    @Test
    void optionalPrismPreviewReflectsPresence() {
        Lens<OptionalRoot, Optional<Integer>> lens = RecordLensBuilder.lens(OptionalRoot.class, OptionalRoot::value);
        Affine<OptionalRoot, Integer> affine = RecordLensBuilder.optional(lens);

        assertTrue(affine.matches(new OptionalRoot(Optional.of(2), 0)));
        assertFalse(affine.matches(new OptionalRoot(Optional.empty(), 0)));
    }

    @Test
    void optionalPrismMatchBridgesToEither() {
        Lens<OptionalRoot, Optional<Integer>> lens = RecordLensBuilder.lens(OptionalRoot.class, OptionalRoot::value);
        Affine<OptionalRoot, Integer> affine = RecordLensBuilder.optional(lens);
        OptionalRoot source = new OptionalRoot(Optional.empty(), 4);

        assertEquals(Either.right(2), match(affine, new OptionalRoot(Optional.of(2), 4)));
        assertEquals(Either.left(source), match(affine, source));
    }

    private static Either<OptionalRoot, Integer> match(Affine<OptionalRoot, Integer> affine, OptionalRoot source) {
        var value = affine.getMaybe(source);
        return value.isDefined() ? Either.right(value.get()) : Either.left(source);
    }

    private record OptionalRoot(Optional<Integer> value, int marker) {
    }
}
