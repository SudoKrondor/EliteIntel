package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.db.dao.ConstructionSiteDao.Requirement;
import elite.intel.db.dao.ConstructionSiteDao.Site;
import elite.intel.db.managers.ConstructionSiteManager;
import elite.intel.gameapi.colonisation.ActiveConstructionSite;
import elite.intel.session.DockedMarket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When the colonisation shopping command is offered to the model at all.
 * <p>
 * The case that drove the rule: the commander stockpiles Steel on their carrier, parks it in the build's
 * system, docks at it, and asks where to get construction cargo. Gated on standing at the depot, the tool
 * was not even in the candidate set - so the request fell through to the ordinary commodity search, which
 * knows nothing about the build and answered "no commodity matching 'construction commodity'".
 */
class FindConstructionCommodityVisibilityTest {

    private static final long DEPOT = 3967232514L;
    private static final long OUR_CARRIER = 3712500736L;

    private final ConstructionSiteManager manager = ConstructionSiteManager.getInstance();
    private final FindConstructionCommodityCommand command = new FindConstructionCommodityCommand();

    @BeforeEach
    @AfterEach
    void clean() {
        manager.clear();
        DockedMarket.getInstance().departed();
    }

    /**
     * The manifest is written on the pad but SPENT everywhere else, so this is the state the command exists
     * to serve, not an edge case.
     */
    @Test
    void offeredWhileDockedAtOurOwnCarrier() {
        visit(DEPOT, Instant.now());
        DockedMarket.getInstance().arrived(OUR_CARRIER);

        assertTrue(command.isVisibleForLLM(null),
                "the build is live and still wants steel - where the ship is parked is beside the point");
    }

    @Test
    void stillOfferedOnTheDepotsOwnPad() {
        visit(DEPOT, Instant.now());
        DockedMarket.getInstance().arrived(DEPOT);

        assertTrue(command.isVisibleForLLM(null));
    }

    @Test
    void offeredInFlight() {
        visit(DEPOT, Instant.now());

        assertTrue(command.isVisibleForLLM(null), "the shopping run is flown, not sat through");
    }

    /**
     * The gate that keeps this apart from the ordinary commodity search: no build, no tool.
     */
    @Test
    void notOfferedWithoutABuild() {
        assertFalse(command.isVisibleForLLM(null));
    }

    @Test
    void notOfferedOnceTheSiteWantsNothing() {
        Site site = siteAt(DEPOT, Instant.now());
        Requirement steel = requirement(2542, 2542);
        manager.save(site, List.of(steel));

        assertFalse(command.isVisibleForLLM(null), "a finished manifest is nothing to go shopping for");
    }

    /**
     * Away from the pad the manifest is only as good as its age - past the forgetting window it is not
     * something to send a commander across the bubble on.
     */
    @Test
    void notOfferedFromAManifestTooOldToBelieve() {
        visit(DEPOT, Instant.now().minus(Duration.ofDays(ActiveConstructionSite.FORGOTTEN_AFTER_DAYS + 1)));

        assertFalse(command.isVisibleForLLM(null));
    }

    /**
     * Saying "I'm done with that build" has to take the tool away with the HUD card, or the commander is
     * still offered shopping trips for a job they abandoned.
     */
    @Test
    void notOfferedAfterTheBuildIsDismissed() {
        visit(DEPOT, Instant.now());
        manager.dismissCurrent();

        assertFalse(command.isVisibleForLLM(null));
    }

    private void visit(long marketId, Instant when) {
        manager.save(siteAt(marketId, when), List.of(requirement(2542, 0)));
    }

    private static Site siteAt(long marketId, Instant when) {
        Site site = new Site();
        site.setMarketId(marketId);
        site.setStationName("Orbital Construction Site: Divis Gateway");
        site.setStarSystem("Hyades Sector NR-V b2-2");
        site.setProgress(0.026187);
        site.setVisitedAt(when.toString());
        return site;
    }

    private static Requirement requirement(int required, int provided) {
        Requirement steel = new Requirement();
        steel.setSymbol("steel");
        steel.setRequiredAmount(required);
        steel.setProvidedAmount(provided);
        steel.setPayment(5057);
        return steel;
    }
}
