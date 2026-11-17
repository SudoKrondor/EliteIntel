package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The percentage of a model load is read from what {@code lms load} prints.
 */
class LoadProgressLineTest {

    @Test
    void aLoadingLineGivesItsPercent() {
        assertEquals(OptionalInt.of(0), LoadProgressLine.percent("Loading google/gemma-4-e4b 0%"));
        assertEquals(OptionalInt.of(42), LoadProgressLine.percent("Loading google/gemma-4-e4b 42%"));
        assertEquals(OptionalInt.of(100), LoadProgressLine.percent("Loading google/gemma-4-e4b 100%"));
    }

    @Test
    void aFractionalPercentIsRoundedDown() {
        assertEquals(OptionalInt.of(7), LoadProgressLine.percent("Loading google/gemma-4-e4b 7.8%"));
    }

    @Test
    void otherLinesGiveNothing() {
        assertEquals(OptionalInt.empty(), LoadProgressLine.percent("> lms load google/gemma-4-e4b --yes"));
        assertEquals(OptionalInt.empty(), LoadProgressLine.percent("Model loaded successfully in 5.2s"));
        assertEquals(OptionalInt.empty(), LoadProgressLine.percent("Loading google/gemma-4-e4b"));
        assertEquals(OptionalInt.empty(), LoadProgressLine.percent("Loading google/gemma-4-e4b 250%"));
        assertEquals(OptionalInt.empty(), LoadProgressLine.percent(null));
    }
}
