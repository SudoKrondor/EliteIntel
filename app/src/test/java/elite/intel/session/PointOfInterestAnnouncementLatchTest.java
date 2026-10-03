package elite.intel.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A bio-sample run lands beside the same point of interest many times in one stay on a planet. The
 * touchdown callout names the point of interest the first time only, until the ship leaves for
 * supercruise or lands somewhere else.
 */
class PointOfInterestAnnouncementLatchTest {

    private final PlayerSession player = PlayerSession.getInstance();

    @BeforeEach
    void freshStay() {
        player.clearAnnouncedPointOfInterest();
    }

    @Test
    void theFirstTouchdownAtAPointOfInterestIsAnnounced() {
        assertTrue(player.markPointOfInterestAnnounced(19, "Distress Beacon"));
    }

    @Test
    void repeatTouchdownsAtTheSamePointOfInterestAreNot() {
        player.markPointOfInterestAnnounced(19, "Distress Beacon");

        assertFalse(player.markPointOfInterestAnnounced(19, "Distress Beacon"));
        assertFalse(player.markPointOfInterestAnnounced(19, "Distress Beacon"));
    }

    @Test
    void aDifferentPointOfInterestOnTheSameBodyIsAnnounced() {
        player.markPointOfInterestAnnounced(19, "Distress Beacon");

        assertTrue(player.markPointOfInterestAnnounced(19, "Crashed Ship"));
    }

    @Test
    void theSamePointOfInterestOnAnotherBodyIsAnnounced() {
        player.markPointOfInterestAnnounced(19, "Distress Beacon");

        assertTrue(player.markPointOfInterestAnnounced(21, "Distress Beacon"));
    }

    @Test
    void leavingForSupercruiseLiftsTheLatch() {
        player.markPointOfInterestAnnounced(19, "Distress Beacon");
        player.clearAnnouncedPointOfInterest();

        assertTrue(player.markPointOfInterestAnnounced(19, "Distress Beacon"));
    }
}
