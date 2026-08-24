package elite.intel.db.managers;

import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which build "the construction site" means, and what saying you are done with it actually does.
 * <p>
 * A squadron can be colonising several systems at once, and one system can hold more than one depot, so the
 * current build is never a lookup by place - it is the pad the commander was last standing on.
 */
class ConstructionSiteCurrencyTest {

    private static final long DIVIS_GATEWAY = 3967232514L;
    private static final long VESPUCCI_LANDING = 3967234818L;

    private final ConstructionSiteManager manager = ConstructionSiteManager.getInstance();

    @BeforeEach
    @AfterEach
    void emptyTheTable() {
        manager.clear();
    }

    private void visit(long marketId, String stationName) {
        Site site = new Site();
        site.setMarketId(marketId);
        site.setStationName(stationName);
        site.setStarSystem("Hyades Sector NR-V b2-2");
        site.setProgress(0.026187);
        site.setVisitedAt(Instant.now().toString());

        Requirement steel = new Requirement();
        steel.setMarketId(marketId);
        steel.setSymbol("steel");
        steel.setRequiredAmount(2542);
        steel.setProvidedAmount(0);
        steel.setPayment(5057);

        manager.save(site, List.of(steel));
    }

    @Test
    void landingAtADepotMakesItTheCurrentBuild() {
        visit(DIVIS_GATEWAY, "Orbital Construction Site: Divis Gateway");

        assertEquals(DIVIS_GATEWAY, manager.currentSite().getMarketId());
    }

    @Test
    void landingAtASecondDepotHandsCurrencyOverToIt() {
        visit(DIVIS_GATEWAY, "Orbital Construction Site: Divis Gateway");
        visit(VESPUCCI_LANDING, "Orbital Construction Site: Vespucci Landing");

        assertEquals(VESPUCCI_LANDING, manager.currentSite().getMarketId(),
                "the build the commander is standing on is the one they mean");
        assertEquals(2, manager.siteCount(), "and the first one is still tracked");
    }

    @Test
    void goingBackToTheFirstDepotMakesItCurrentAgain() {
        visit(DIVIS_GATEWAY, "Orbital Construction Site: Divis Gateway");
        visit(VESPUCCI_LANDING, "Orbital Construction Site: Vespucci Landing");
        visit(DIVIS_GATEWAY, "Orbital Construction Site: Divis Gateway");

        assertEquals(DIVIS_GATEWAY, manager.currentSite().getMarketId());
    }

    /**
     * The whole point of the explicit flag. Saying "I'm done" has to leave the HUD quiet, not promote
     * whichever build happened to be visited before this one - that reads as the card refusing to go away.
     */
    @Test
    void dismissingLeavesNoCurrentBuildAtAll() {
        visit(DIVIS_GATEWAY, "Orbital Construction Site: Divis Gateway");
        visit(VESPUCCI_LANDING, "Orbital Construction Site: Vespucci Landing");

        manager.dismissCurrent();

        assertNull(manager.currentSite());
    }

    /**
     * Nothing is deleted. Throwing away the records of a squadron's other builds because the commander said
     * they were done with this one would be a much bigger answer than the question.
     */
    @Test
    void dismissingKeepsEveryManifest() {
        visit(DIVIS_GATEWAY, "Orbital Construction Site: Divis Gateway");
        visit(VESPUCCI_LANDING, "Orbital Construction Site: Vespucci Landing");

        manager.dismissCurrent();

        assertEquals(2, manager.siteCount(), "both builds are still tracked");
        assertNotNull(manager.findSite(DIVIS_GATEWAY));
        assertNotNull(manager.findSite(VESPUCCI_LANDING));
        assertEquals(2542, manager.requirements(DIVIS_GATEWAY).getFirst().getRequiredAmount(),
                "and the manifests they collected are untouched");
    }

    @Test
    void landingAgainAfterDismissingBringsThatBuildBack() {
        visit(DIVIS_GATEWAY, "Orbital Construction Site: Divis Gateway");
        manager.dismissCurrent();

        visit(DIVIS_GATEWAY, "Orbital Construction Site: Divis Gateway");

        assertEquals(DIVIS_GATEWAY, manager.currentSite().getMarketId());
    }

    @Test
    void beforeAnyDepotHasBeenVisitedThereIsNoCurrentBuild() {
        assertNull(manager.currentSite());
    }

    /**
     * Landing back at a build whose manifest has not moved. The depot event is fingerprinted, so nothing is
     * rewritten - and currency must not be hanging off that write, or the commander flies back to the first
     * site and every answer stays about the second.
     */
    @Test
    void standingOnAPadMakesItCurrentWithoutRewritingTheManifest() {
        visit(DIVIS_GATEWAY, "Orbital Construction Site: Divis Gateway");
        visit(VESPUCCI_LANDING, "Orbital Construction Site: Vespucci Landing");

        manager.arrivedAt(DIVIS_GATEWAY, Instant.now().toString());

        assertEquals(DIVIS_GATEWAY, manager.currentSite().getMarketId());
        assertEquals(1, manager.requirements(DIVIS_GATEWAY).size(), "the manifest is untouched");
    }

    /**
     * An identical republish is still a fresh reading of the site's own panel, so the AS OF caveat and the
     * forgetting window both move with it.
     */
    @Test
    void standingOnAPadRefreshesWhenTheManifestWasLastSeen() {
        Site old = new Site();
        old.setMarketId(DIVIS_GATEWAY);
        old.setStationName("Orbital Construction Site: Divis Gateway");
        old.setStarSystem("Hyades Sector NR-V b2-2");
        old.setProgress(0.5);
        old.setVisitedAt(Instant.now().minus(Duration.ofHours(30)).toString());
        manager.save(old, List.of());

        String now = Instant.now().toString();
        manager.arrivedAt(DIVIS_GATEWAY, now);

        assertEquals(now, manager.findSite(DIVIS_GATEWAY).getVisitedAt());
    }

    /**
     * The first manifest of a site we have never stored is written by the depot event itself; there is no
     * row to mark current yet, and inventing one would leave a nameless site in the table.
     */
    @Test
    void arrivingAtASiteWeHaveNeverStoredDoesNothing() {
        manager.arrivedAt(DIVIS_GATEWAY, Instant.now().toString());

        assertNull(manager.currentSite());
        assertEquals(0, manager.siteCount());
    }
}
