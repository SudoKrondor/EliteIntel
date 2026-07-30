package elite.intel.junit.util;

import elite.intel.util.SystemAddressCoordinates;
import elite.intel.util.SystemAddressCoordinates.BoxelCentre;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vectors are real SystemAddress/StarPos pairs taken from journal files, one per mass code the
 * commander actually meets, so each case pins the decode against the position the game itself
 * reported for that system. The assertion is the guarantee the decoder makes and no more: the true
 * position lies inside the boxel, so no axis is off by more than half an edge.
 */
class SystemAddressCoordinatesTest {

    @Test
    @DisplayName("a 20 ly boxel: the uncharted carrier system that started this")
    void decodesAMassCodeBSystem() {
        // Eephaik CX-V b31-9 - uncharted, EDSM has never heard of it, no StarPos event for it.
        assertWithinBoxel(20299220533521L, -12135.0, -895.0, 17545.0, 20);
    }

    @Test
    @DisplayName("hand-authored systems decode the same way")
    void decodesHandAuthoredSystems() {
        // Gnowee, StarPos [22.90625, -4.71875, 40.5]
        assertNear(358797546202L, 22.90625, -4.71875, 40.5);
        // 78 Iota Leonis, StarPos [-7.75, 55.25, 20.9375]
        assertNear(216059086004L, -7.75, 55.25, 20.9375);
    }

    @Test
    @DisplayName("procedural systems out in the black decode against their own StarPos")
    void decodesProceduralSystemsAgainstTheirStarPos() {
        // Eephaik TE-W c16-42, StarPos [-12071.59375, -890.71875, 17598.90625]
        assertNear(11608457027730L, -12071.59375, -890.71875, 17598.90625);
        // LP 734-11, StarPos [43.5, 44.75, 3.4375]
        assertNear(20462700799401L, 43.5, 44.75, 3.4375);
    }

    @Test
    @DisplayName("no address, nothing to decode")
    void rejectsAnAbsentAddress() {
        assertTrue(SystemAddressCoordinates.decode(null).isEmpty());
        assertTrue(SystemAddressCoordinates.decode(0L).isEmpty());
        assertTrue(SystemAddressCoordinates.decode(-1L).isEmpty());
    }

    @Test
    @DisplayName("the error bound is half the boxel edge")
    void reportsItsOwnPrecision() {
        BoxelCentre centre = SystemAddressCoordinates.decode(20299220533521L).orElseThrow();
        assertEquals(20, centre.edgeLy());
        assertEquals(10.0, centre.maxErrorLy());
    }

    private static void assertWithinBoxel(long systemAddress, double x, double y, double z, int edgeLy) {
        BoxelCentre centre = SystemAddressCoordinates.decode(systemAddress).orElseThrow();
        assertEquals(edgeLy, centre.edgeLy(), "boxel edge");
        assertEquals(x, centre.x(), 1e-9, "x");
        assertEquals(y, centre.y(), 1e-9, "y");
        assertEquals(z, centre.z(), 1e-9, "z");
    }

    /**
     * The decoded centre must sit within half a boxel edge of the position the game reported.
     */
    private static void assertNear(long systemAddress, double x, double y, double z) {
        Optional<BoxelCentre> decoded = SystemAddressCoordinates.decode(systemAddress);
        assertTrue(decoded.isPresent(), "decodable address");
        BoxelCentre centre = decoded.get();
        double tolerance = centre.maxErrorLy();
        assertEquals(x, centre.x(), tolerance, "x within half a boxel");
        assertEquals(y, centre.y(), tolerance, "y within half a boxel");
        assertEquals(z, centre.z(), tolerance, "z within half a boxel");
    }
}
