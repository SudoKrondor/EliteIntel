package elite.intel.util;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignedAndDecimalNumbersTest {

    @Test
    void spellsTheMinusSignOfANegativeNumber() {
        assertEquals("altitude minus two hundred thirty-three metres",
                SignedAndDecimalNumbers.inWords("altitude -233 metres", Language.EN));
    }

    @Test
    void keepsEveryFractionDigitOfACoordinate() {
        assertEquals("Starting navigation to latitude minus sixty-two point one two three, longitude thirty-four point four five three.",
                SignedAndDecimalNumbers.inWords("Starting navigation to latitude -62.123, longitude 34.453.", Language.EN));
    }

    @Test
    void saysTheSignInTheTargetLanguage() {
        assertTrue(SignedAndDecimalNumbers.inWords("-5", Language.RU).startsWith("минус"));
        assertTrue(SignedAndDecimalNumbers.inWords("-5", Language.DE).startsWith("minus"));
    }

    @Test
    void leavesPlainWholeNumbersAsDigits() {
        assertEquals("set speed to 75 and jump 3 times",
                SignedAndDecimalNumbers.inWords("set speed to 75 and jump 3 times", Language.EN));
    }

    @Test
    void leavesSystemNamesAndVersionsAlone() {
        assertEquals("Col 285 Sector XY-Z c12-34 runs v1.1.0021",
                SignedAndDecimalNumbers.inWords("Col 285 Sector XY-Z c12-34 runs v1.1.0021", Language.EN));
        assertEquals("build 1.1.0021", SignedAndDecimalNumbers.inWords("build 1.1.0021", Language.EN));
    }
}
