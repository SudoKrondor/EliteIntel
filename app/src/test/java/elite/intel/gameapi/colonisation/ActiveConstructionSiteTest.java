package elite.intel.gameapi.colonisation;

import elite.intel.db.dao.ConstructionSiteDao.Site;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When a build stops being volunteered.
 * <p>
 * Hauling to a colony has no end event in the journal: the commander does a few runs and then goes trading,
 * and nothing ever says they are finished. The card would otherwise sit on screen for weeks.
 */
class ActiveConstructionSiteTest {

    private static Site visitedHoursAgo(long hours) {
        Site site = new Site();
        site.setMarketId(3967232514L);
        site.setStationName("Orbital Construction Site: Divis Gateway");
        site.setStarSystem("Hyades Sector NR-V b2-2");
        site.setProgress(0.026187);
        site.setVisitedAt(Instant.now().minus(hours, ChronoUnit.HOURS).toString());
        return site;
    }

    @Test
    void aBuildVisitedTodayIsLive() {
        assertTrue(ActiveConstructionSite.isLive(visitedHoursAgo(2)));
    }

    /**
     * Generous on purpose: a commander who plays at the weekend should find their build where they left it.
     */
    @Test
    void aBuildVisitedTheDayBeforeYesterdayIsStillLive() {
        assertTrue(ActiveConstructionSite.isLive(visitedHoursAgo(47)));
    }

    /**
     * Past this the tonnages have had long enough to be wrong - other commanders haul to the same depot -
     * that showing them is worse than showing nothing.
     */
    @Test
    void aBuildNotVisitedInDaysIsNoLongerVolunteered() {
        assertFalse(ActiveConstructionSite.isLive(
                visitedHoursAgo(ActiveConstructionSite.FORGOTTEN_AFTER_DAYS * 24 + 1)));
    }

    @Test
    void aFinishedBuildIsNotLiveHoweverRecentTheVisit() {
        Site finished = visitedHoursAgo(1);
        finished.setComplete(true);

        assertFalse(ActiveConstructionSite.isLive(finished));
    }

    @Test
    void aFailedBuildIsNotLiveEither() {
        Site failed = visitedHoursAgo(1);
        failed.setFailed(true);

        assertFalse(ActiveConstructionSite.isLive(failed));
    }

    @Test
    void noBuildAtAllIsNotLive() {
        assertFalse(ActiveConstructionSite.isLive(null));
    }

    /**
     * A site first seen after a restart carries a manifest and no timestamp we can read. Treating that as
     * ancient would hide a build the commander is standing on; {@link ManifestAge} reads it as current.
     */
    @Test
    void aSiteWithNoReadableVisitTimeIsTreatedAsCurrent() {
        Site noTimestamp = visitedHoursAgo(1);
        noTimestamp.setVisitedAt(null);

        assertTrue(ActiveConstructionSite.isLive(noTimestamp));
    }
}
