package elite.intel.junit.gameapi;

import elite.intel.gameapi.FireGroups;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the argument the LLM hands {@code select_fire_group_by_nato}.
 *
 * <p>"group bravo" regressed because {@link FireGroups#fireGroupByNato} matched only the exact lower-case
 * NATO word and mapped everything else to 0. Any other form the model emits ("Bravo", "B", "2") therefore
 * switched to group A instead of B - silently, since the caller's only no-op signal is -1 and the old code
 * returned that solely for a null argument.
 */
class FireGroupsTest {

    @ParameterizedTest(name = "\"{0}\" resolves to group {1}")
    @CsvSource({
            "alpha, 0", "bravo, 1", "charlie, 2", "delta, 3",
            "echo, 4", "foxtrot, 5", "golf, 6", "hotel, 7",
    })
    void aNatoWordResolvesToItsGroup(String argument, int expected) {
        assertEquals(expected, FireGroups.fireGroupByNato(argument));
    }

    @ParameterizedTest(name = "\"{0}\" resolves to group 1")
    @ValueSource(strings = {"bravo", "Bravo", "BRAVO", "  bravo  ", "B", "b", "2"})
    void theModelsSpellingOfBravoStillResolvesToBravo(String argument) {
        assertEquals(1, FireGroups.fireGroupByNato(argument));
    }

    @ParameterizedTest(name = "\"{0}\" is a no-op")
    @ValueSource(strings = {"zulu", "india", "x", "9", "0", "fire group", "  "})
    void anUnrecognizedArgumentIsANoOpRatherThanGroupA(String argument) {
        assertEquals(-1, FireGroups.fireGroupByNato(argument),
                "an unmapped argument must not silently switch to group A");
    }

    @ParameterizedTest(name = "{0} is a no-op")
    @NullAndEmptySource
    void aMissingArgumentIsANoOp(String argument) {
        assertEquals(-1, FireGroups.fireGroupByNato(argument));
    }

    /**
     * Groups beyond H are unbound in the game's own UI, so they must not resolve.
     */
    @Test
    void groupsBeyondHDoNotResolve() {
        assertEquals(-1, FireGroups.fireGroupByNato("india"));
        assertEquals(-1, FireGroups.fireGroupByNato("i"));
    }
}
