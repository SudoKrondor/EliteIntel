package elite.intel.gameapi.bookmarks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Surface guidance knows no body, so a bookmarked spot waits for its own planet and is handed over there only.
 */
class PendingSurfaceTargetTest {

    private final PendingSurfaceTarget pending = PendingSurfaceTarget.getInstance();
    private final LocationBookmark camp = LocationBookmark.surface("Pending Test System", "Pending Test System 3 a", 1.5, 2.5);

    @AfterEach
    void clear() {
        pending.clear();
    }

    @Test
    void anotherPlanetDoesNotTakeIt() {
        pending.arm(camp);

        assertTrue(pending.takeIfOn("Pending Test System", "Pending Test System 3 b").isEmpty());
        assertTrue(pending.peek().isPresent(), "still waiting for its own planet");
    }

    @Test
    void itsPlanetTakesItOnce() {
        pending.arm(camp);

        assertEquals(camp, pending.takeIfOn("pending test system", "PENDING TEST SYSTEM 3 A").orElseThrow());
        assertTrue(pending.takeIfOn("Pending Test System", "Pending Test System 3 a").isEmpty());
    }

    @Test
    void aRouteToItsOwnSystemKeepsItAndAnyOtherDropsIt() {
        pending.arm(camp);

        pending.routePlottedTo("Pending Test System");
        assertTrue(pending.peek().isPresent());

        pending.routePlottedTo("Somewhere Else");
        assertTrue(pending.peek().isEmpty());
    }

    @Test
    void onlyASurfaceSpotCanWait() {
        assertThrows(IllegalArgumentException.class, () -> pending.arm(LocationBookmark.planet("Sol", "Earth")));
    }
}
