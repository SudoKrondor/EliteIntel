package elite.intel.ui.screen.bindings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Input codes in the player's words - never {@code Joy_POV2Up} on screen where "Hat 2, up" will do. */
class InputNamesTest {

    @Test
    void buttonsHatsAndAxesAreSaidInWords() {
        assertEquals("Button 12", InputNames.spoken("Joy_12"));
        assertEquals("Hat 2, up", InputNames.spoken("Joy_POV2Up"));
        assertEquals("X axis", InputNames.spoken("Joy_XAxis"));
        assertEquals("Z rotation axis", InputNames.spoken("Joy_RZAxis"));
    }

    @Test
    void inputsAreListedTheWayAPlayerCountsThem() {
        java.util.List<String> sorted = new java.util.ArrayList<>(java.util.List.of("Joy_RZAxis", "Joy_12", "Joy_POV2Up",
                "Joy_4", "Joy_1", "Joy_POV1Down"));
        sorted.sort(InputNames.ORDER);
        assertEquals(java.util.List.of("Joy_1", "Joy_4", "Joy_12", "Joy_POV1Down", "Joy_POV2Up", "Joy_RZAxis"), sorted);
    }

    @Test
    void aCodeItDoesNotKnowIsGivenBackAsItIs() {
        assertEquals("Key_Space", InputNames.spoken("Key_Space"));
    }
}
