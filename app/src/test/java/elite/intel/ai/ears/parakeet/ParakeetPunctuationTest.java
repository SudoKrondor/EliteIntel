package elite.intel.ai.ears.parakeet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParakeetPunctuationTest {

    @Test
    void keepsTheDecimalPointOfCoordinates() {
        assertEquals("navigate to latitude 62.123 longitude -34.453",
                ParakeetSTTImpl.stripPunctuation("navigate to latitude 62.123, longitude -34.453."));
    }

    @Test
    void keepsGroupingCommasInsideANumber() {
        assertEquals("sell for 1,000,000 credits", ParakeetSTTImpl.stripPunctuation("sell for 1,000,000 credits."));
    }

    @Test
    void dropsSentencePunctuationAfterADigit() {
        assertEquals("set speed to 75 now", ParakeetSTTImpl.stripPunctuation("set speed to 75. now!"));
    }

    @Test
    void dropsSentencePunctuationBetweenWords() {
        assertEquals("okay fire lasers what", ParakeetSTTImpl.stripPunctuation("okay, fire lasers; what?:"));
    }
}
