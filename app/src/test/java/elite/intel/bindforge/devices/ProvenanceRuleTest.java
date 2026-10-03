package elite.intel.bindforge.devices;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The rule that decides whether BindForge may touch an entry. It is a permission, so these spell out the
 * reasoning rather than only the outcomes.
 */
class ProvenanceRuleTest {

    private final FrontierStockDevices stock = FrontierStockDevices.getInstance();

    @Test
    void anEntryFrontierShipsIsTheirs() {
        assertEquals(Provenance.FRONTIER, ProvenanceRule.of("T-Rudder", stock));
        assertEquals(Provenance.FRONTIER, ProvenanceRule.of("GamePad", stock));
        assertEquals(Provenance.FRONTIER, ProvenanceRule.of("VPCPanel", stock),
                "looks like a personal addition, and is Frontier's");
    }

    /**
     * The whole point of the three-state rule. These two really are the developer's own sticks - but the
     * scan cannot know that, only that Frontier did not ship them as of the capture. Claiming them would be
     * guessing, and the same guess would misclaim any device Frontier adds after the capture.
     */
    @Test
    void anEntryAbsentFromFrontiersListIsUnknownRatherThanTheUsers() {
        assertEquals(Provenance.UNKNOWN, ProvenanceRule.of("LVWAP", stock));
        assertEquals(Provenance.UNKNOWN, ProvenanceRule.of("RVWAP", stock));
    }

    /**
     * A device Frontier adds support for after this capture is the case the third state protects against.
     * Under a two-state rule it would be recorded as the player's, after which BindForge could offer to
     * rename or clear an entry belonging to the game.
     */
    @Test
    void aDeviceFrontierShipsAfterTheCaptureIsNotClaimedForTheUser() {
        assertEquals(Provenance.UNKNOWN, ProvenanceRule.of("SomeStickShippedNextPatch", stock));
    }

    /** Never produced by reading a file - it is set when the user confirms the entry is theirs. */
    @Test
    void theScanNeverClaimsAnEntryAsUserPreexisting() {
        for (String name : new String[]{"LVWAP", "T-Rudder", "Anything", ""}) {
            assertNotEquals(Provenance.USER_PREEXISTING, ProvenanceRule.of(name, stock),
                    "'" + name + "' must not be claimed for the user by a read");
        }
    }
}
