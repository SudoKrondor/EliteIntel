package elite.intel.gameapi.signals;

import elite.intel.gameapi.missions.ResourceSiteProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Counting a system's resource extraction sites out of a burst of signals.
 * <p>
 * The two things that go wrong here are both silent. Reading the localised name instead of the symbol
 * works perfectly until the commander runs an English app against a German game client, at which point
 * every site stops being counted. And counting across arrivals instead of within one turns Sol's five
 * low sites into forty, because the game re-announces the whole set every time the ship drops in.
 */
class ResourceSiteSweepTest {

    private static final String STANDARD = "$MULTIPLAYER_SCENARIO14_TITLE;";
    private static final String LOW = "$MULTIPLAYER_SCENARIO77_TITLE;";
    private static final String HIGH = "$MULTIPLAYER_SCENARIO78_TITLE;";
    private static final String HAZARDOUS = "$MULTIPLAYER_SCENARIO79_TITLE;";

    @Test
    void everyGradeIsRecognisedByItsSymbol() {
        assertEquals(ResourceSiteGrade.STANDARD, ResourceSiteGrade.fromSymbol(STANDARD));
        assertEquals(ResourceSiteGrade.LOW, ResourceSiteGrade.fromSymbol(LOW));
        assertEquals(ResourceSiteGrade.HIGH, ResourceSiteGrade.fromSymbol(HIGH));
        assertEquals(ResourceSiteGrade.HAZARDOUS, ResourceSiteGrade.fromSymbol(HAZARDOUS));
    }

    @Test
    void aSignalThatIsNotAResourceSiteIsNotOne() {
        assertNull(ResourceSiteGrade.fromSymbol("$MULTIPLAYER_SCENARIO42_TITLE;"),
                "a scenario this enum has not met is not silently counted as a low site");
        assertNull(ResourceSiteGrade.fromSymbol("Resource Extraction Site [Low]"),
                "the localised name is the game client's language, not ours - only the symbol counts");
        assertNull(ResourceSiteGrade.fromSymbol(null));
    }

    @Test
    void oneSweepCountsEverySiteItReports() {
        ResourceSiteSweep sweep = new ResourceSiteSweep();

        sweep.add("42@t1", ResourceSiteGrade.HAZARDOUS);
        sweep.add("42@t1", ResourceSiteGrade.HIGH);
        sweep.add("42@t1", ResourceSiteGrade.HAZARDOUS);
        ResourceSiteProfile profile = sweep.add("42@t1", ResourceSiteGrade.LOW);

        assertEquals(new ResourceSiteProfile(0, 1, 1, 2), profile);
    }

    @Test
    void aSecondArrivalStartsCountingOver() {
        ResourceSiteSweep sweep = new ResourceSiteSweep();

        sweep.add("42@t1", ResourceSiteGrade.LOW);
        sweep.add("42@t1", ResourceSiteGrade.LOW);
        ResourceSiteProfile second = sweep.add("42@t2", ResourceSiteGrade.LOW);

        assertEquals(1, second.low(),
                "every arrival re-announces the same sites, so summing them invents sites that are not there");
    }

    @Test
    void theBestOfTwoPartialSweepsKeepsBothFindings() {
        ResourceSiteProfile hazardousOnly = new ResourceSiteProfile(0, 0, 0, 2);
        ResourceSiteProfile lowOnly = new ResourceSiteProfile(1, 4, 0, 0);

        assertEquals(new ResourceSiteProfile(1, 4, 0, 2), hazardousOnly.max(lowOnly),
                "one pass may catch the hazardous sites and another the low ones - the system has both");
    }

    @Test
    void aGroundCarriedOverFromTheOldListHasSitesButNoKnownGrades() {
        ResourceSiteProfile carriedOver = new ResourceSiteProfile(0, 0, 0, 0);

        assertFalse(carriedOver.gradesKnown(),
                "the hand-confirmation workflow recorded that a system had sites, never which - and "
                        + "that has to stay distinguishable from a system with none");
    }
}
