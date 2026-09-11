package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The arrival line and the road ahead are separate toggles. A commander who turned "Announce arrival"
 * off must not hear "Arrived at" any more, and must still get the jumps-left line when they kept that
 * one on - the point of the extra toggle is exactly that combination.
 */
class WaypointArrivalAnnouncementTest {

    private static final List<NavRouteDto> TWO_LEGS_LEFT = List.of(leg("Jackson's Lighthouse", "N"), leg("Colonia", "K"));

    @BeforeEach
    void forceEnglishLocale() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @AfterEach
    void restoreEnglishLocale() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void bothOnSpeaksTheArrivalThenTheRoadAhead() {
        String line = JumpCompletedSubscriber.waypointArrival("Sol", TWO_LEGS_LEFT, true, true, () -> false);

        assertEquals("Arrived at Sol. Next Waypoint: Jackson's Lighthouse, Star Class: N. 2 jumps left to destination.", line);
    }

    @Test
    void arrivalOffStillSpeaksTheRoadAhead() {
        String line = JumpCompletedSubscriber.waypointArrival("Sol", TWO_LEGS_LEFT, false, true, () -> false);

        assertFalse(line.contains("Arrived"), "the arrival was switched off: " + line);
        assertTrue(line.startsWith("Next Waypoint: Jackson's Lighthouse"), "the road ahead must not start with a stray space: " + line);
        assertTrue(line.endsWith("2 jumps left to destination."), line);
    }

    @Test
    void remainingJumpsOffStillSpeaksTheArrival() {
        String line = JumpCompletedSubscriber.waypointArrival("Sol", TWO_LEGS_LEFT, true, false, () -> false);

        assertEquals("Arrived at Sol.", line);
    }

    /**
     * An empty line is the caller's cue to skip the narrator entirely, rather than asking the LLM to
     * announce nothing.
     */
    @Test
    void bothOffSaysNothing() {
        assertEquals("", JumpCompletedSubscriber.waypointArrival("Sol", TWO_LEGS_LEFT, false, false, () -> true));
    }

    /**
     * The fuel answer costs a loadout lookup, so it is only asked for when the road ahead is spoken.
     */
    @Test
    void fuelIsNotLookedUpWhenTheRoadAheadIsSilent() {
        JumpCompletedSubscriber.waypointArrival("Sol", TWO_LEGS_LEFT, true, false, () -> {
            throw new AssertionError("fuel scoop consulted for a line that never mentions the next star");
        });
    }

    @Test
    void fuelClauseFollowsTheNextStarWhenWanted() {
        String line = JumpCompletedSubscriber.waypointArrival("Sol", List.of(leg("Alioth", "A")), false, true, () -> true);

        assertTrue(line.startsWith("Next Waypoint: Alioth, Star Class: A. Refuel possible."), line);
        assertTrue(line.endsWith("1 jump left to destination."), line);
    }

    private static NavRouteDto leg(String name, String starClass) {
        NavRouteDto leg = new NavRouteDto();
        leg.setName(name);
        leg.setStarClass(starClass);
        return leg;
    }
}
