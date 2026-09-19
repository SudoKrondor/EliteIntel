package elite.intel.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A narration payload that ended its list with a trailing ", " - "Remaining genus: Bacterium, Clypeus, Concha,
 * Fungoida, Stratum, Tussock, " - was spoken as "... Stratum, Tussock, and." The model was told to repeat the list
 * exactly and finished the sentence it saw coming. The list must close itself.
 */
class StringUtlsSpokenListTest {

    @Test
    void closesTheListWithAFullStopAndNoTrailingSeparator() {
        assertEquals("Bacterium, Clypeus, Concha, Fungoida, Stratum, Tussock.",
                StringUtls.spokenList(List.of("Bacterium", "Clypeus", "Concha", "Fungoida", "Stratum", "Tussock")));
    }

    @Test
    void aSingleItemIsJustTheItem() {
        assertEquals("Osseus.", StringUtls.spokenList(List.of("Osseus")));
    }

    @Test
    void blankEntriesCannotReintroduceADanglingSeparator() {
        assertEquals("Stratum, Tussock.", StringUtls.spokenList(Arrays.asList("Stratum", null, " ", "Tussock ")));
    }

    @Test
    void nothingToListIsEmptyNotAStop() {
        assertEquals("", StringUtls.spokenList(List.of()));
    }
}
