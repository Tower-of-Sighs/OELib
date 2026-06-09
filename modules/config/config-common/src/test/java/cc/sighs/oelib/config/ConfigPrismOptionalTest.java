package cc.sighs.oelib.config;

import cc.sighs.oelib.config.optics.ConfigAffine;
import cc.sighs.oelib.config.optics.ConfigLens;
import com.mojang.datafixers.util.Either;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigPrismOptionalTest {

    @Test
    void optionalPrismUpdatesOnlyWhenPresent() {
        ConfigLens<OptionalRoot, Optional<Integer>> lens = RecordLensBuilder.lens(OptionalRoot.class, OptionalRoot::value);
        ConfigAffine<OptionalRoot, Integer> affine = RecordLensBuilder.optional(lens);

        OptionalRoot present = new OptionalRoot(Optional.of(5), 9);
        OptionalRoot absent = new OptionalRoot(Optional.empty(), 9);

        OptionalRoot presentUpdated = affine.updateIfPresent(present, value -> value + 3);
        OptionalRoot absentUpdated = affine.updateIfPresent(absent, value -> value + 3);

        assertEquals(Optional.of(8), presentUpdated.value());
        assertEquals(9, presentUpdated.marker());
        assertEquals(Optional.empty(), absentUpdated.value());
        assertEquals(9, absentUpdated.marker());
    }

    @Test
    void optionalPrismStressLoopKeepsCorrectValue() {
        ConfigLens<OptionalRoot, Optional<Integer>> lens = RecordLensBuilder.lens(OptionalRoot.class, OptionalRoot::value);
        ConfigAffine<OptionalRoot, Integer> affine = RecordLensBuilder.optional(lens);

        OptionalRoot current = new OptionalRoot(Optional.of(1), 1);
        for (int i = 0; i < 50_000; i++) {
            current = affine.updateIfPresent(current, value -> value + 1);
        }

        assertEquals(Optional.of(50_001), current.value());
    }

    @Test
    void optionalPrismPreviewReflectsPresence() {
        ConfigLens<OptionalRoot, Optional<Integer>> lens = RecordLensBuilder.lens(OptionalRoot.class, OptionalRoot::value);
        ConfigAffine<OptionalRoot, Integer> affine = RecordLensBuilder.optional(lens);

        assertTrue(affine.preview(new OptionalRoot(Optional.of(2), 0)).isPresent());
        assertTrue(affine.preview(new OptionalRoot(Optional.empty(), 0)).isEmpty());
    }

    @Test
    void optionalPrismMatchBridgesToEither() {
        ConfigLens<OptionalRoot, Optional<Integer>> lens = RecordLensBuilder.lens(OptionalRoot.class, OptionalRoot::value);
        ConfigAffine<OptionalRoot, Integer> affine = RecordLensBuilder.optional(lens);
        OptionalRoot source = new OptionalRoot(Optional.empty(), 4);

        assertEquals(Either.right(2), affine.match(new OptionalRoot(Optional.of(2), 4)));
        assertEquals(Either.left(source), affine.match(source));
    }

    private record OptionalRoot(Optional<Integer> value, int marker) {
    }
}
