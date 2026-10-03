package elite.intel.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Grok answered "reduce speed by one" with {@code {"key":1.0}}, and stripping the dot out of the digits read
 * that as ten: ten throttle taps and ten beeps (commander bundle 2026-09-30). The fraction is cut, not read.
 */
class StringUtlsGetIntSafelyTest {

    @Test
    void aWholeNumberWrittenAsADecimalKeepsItsValue() {
        assertEquals(1, StringUtls.getIntSafely("1.0"));
        assertEquals(25, StringUtls.getIntSafely("25.00"));
    }

    @Test
    void aFractionIsCutNotRounded() {
        assertEquals(2, StringUtls.getIntSafely("2.5"));
        assertEquals(6, StringUtls.getIntSafely("6.9"));
    }

    @Test
    void groupingSeparatorsAndWordsAroundTheNumberAreStillDropped() {
        assertEquals(1_000_000, StringUtls.getIntSafely("1,000,000 credits"));
        assertEquals(1_000_000, StringUtls.getIntSafely("1,000,000.50"));
        assertEquals(70, StringUtls.getIntSafely("70 percent"));
        assertEquals(25, StringUtls.getIntSafely("25"));
    }

    @Test
    void noDigitsAtAllIsNoNumber() {
        assertNull(StringUtls.getIntSafely("as fast as possible"));
        assertNull(StringUtls.getIntSafely(null));
    }
}
