package elite.intel.devices.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The tokens are the game's own, {@code Joy_XAxis} to {@code Joy_VAxis}. */
class GameAxisTest {

    @Test
    void theTokenIsTheGames() {
        assertEquals("Joy_XAxis", GameAxis.X.token());
        assertEquals("Joy_RZAxis", GameAxis.RZ.token());
        assertEquals("Joy_VAxis", GameAxis.V.token());
    }
}
