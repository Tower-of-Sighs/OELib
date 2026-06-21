package cc.sighs.oelib.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConfigStressTest {

    @Test
    void deepLensUpdateRemainsCorrectUnderHighIteration() {
        var l1 = RecordLensBuilder.lens(StressRoot.class, StressRoot::level1);
        var l2 = RecordLensBuilder.lens(Level1.class, Level1::level2);
        var l3 = RecordLensBuilder.lens(Level2.class, Level2::level3);
        var l4 = RecordLensBuilder.lens(Level3.class, Level3::level4);
        var valueLens = RecordLensBuilder.lens(Level4.class, Level4::value);
        var deepValueLens = l1.andThen(l2).andThen(l3).andThen(l4).andThen(valueLens);

        StressRoot current = new StressRoot(new Level1(new Level2(new Level3(new Level4(0, false), 9), 1.0), "a"), "meta");
        for (int i = 0; i < 20_000; i++) {
            current = deepValueLens.modify(value -> value + 1, current);
        }

        assertEquals(20_000, current.level1().level2().level3().level4().value());
        assertEquals("meta", current.meta());
    }

    @Test
    void multiLensMutationsKeepIndependentFieldsStable() {
        var l1 = RecordLensBuilder.lens(StressRoot.class, StressRoot::level1);
        var l2 = RecordLensBuilder.lens(Level1.class, Level1::level2);
        var l3 = RecordLensBuilder.lens(Level2.class, Level2::level3);
        var l4 = RecordLensBuilder.lens(Level3.class, Level3::level4);

        var valueLens = l1.andThen(l2).andThen(l3).andThen(l4).andThen(RecordLensBuilder.lens(Level4.class, Level4::value));
        var flagLens = l1.andThen(l2).andThen(l3).andThen(l4).andThen(RecordLensBuilder.lens(Level4.class, Level4::flag));
        var ratioLens = l1.andThen(l2).andThen(RecordLensBuilder.lens(Level2.class, Level2::ratio));
        var noteLens = l1.andThen(RecordLensBuilder.lens(Level1.class, Level1::note));

        StressRoot current = new StressRoot(new Level1(new Level2(new Level3(new Level4(5, false), 7), 1.5), "note"), "m");
        for (int i = 0; i < 5_000; i++) {
            current = valueLens.modify(value -> value + 2, current);
            current = flagLens.modify(value -> !value, current);
            current = ratioLens.modify(value -> value + 0.01, current);
            current = noteLens.modify(value -> value + ".", current);
        }

        assertEquals(10_005, current.level1().level2().level3().level4().value());
        assertFalse(current.level1().level2().level3().level4().flag());
        assertEquals(51.5, current.level1().level2().ratio(), 1e-9);
        assertEquals(5_004, current.level1().note().length());
    }

    private record StressRoot(Level1 level1, String meta) {
    }

    private record Level1(Level2 level2, String note) {
    }

    private record Level2(Level3 level3, double ratio) {
    }

    private record Level3(Level4 level4, int pad) {
    }

    private record Level4(int value, boolean flag) {
    }
}
