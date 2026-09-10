package elite.intel.ui.overlay;

import elite.intel.db.managers.BountyManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.signals.ResourceSiteGrade;
import elite.intel.session.ResourceSite;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static elite.intel.ui.overlay.HudCards.valueOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The card for diving a resource site with no contract to show for it.
 * <p>
 * The behaviour worth guarding is the shape of the hunt rather than the rows: it has to survive the
 * loop the commander actually flies - drop in, clear it, boost out, drop into the next one, fly to a
 * station and cash in - without blinking off in the middle, and it has to get out of the way the
 * moment the commander takes on work they can be held to.
 */
class BountyHuntCardTest {

    private static final long HERE = 5031789073122L;
    private static final long SOMEWHERE_ELSE = 5306666980066L;

    private final MissionManager missions = MissionManager.getInstance();
    private final BountyManager bounties = BountyManager.getInstance();
    private final ResourceSite resourceSite = ResourceSite.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void clean() {
        missions.clear();
        bounties.clear();
        resourceSite.cashedIn();
    }

    @Test
    void droppingIntoAResourceSitePutsTheHuntOnScreen() {
        resourceSite.droppedIn(HERE, ResourceSiteGrade.HAZARDOUS, 4);
        claim(1, 250_000);
        claim(2, 431_500);

        HudObjective card = cardHere().orElseThrow();

        assertEquals("bounty-hunt", card.id());
        assertEquals(HudObjective.PRIORITY_AMBIENT, card.priority(),
                "the commander never accepted this the way they accept a contract");
        assertEquals("ODOMAZOTZ - HAZ RES", card.subtitle());
        assertEquals(HudText.credits(681_500), valueOf(card, HudText.get("overlay.card.row.bounties")));
        assertEquals("2", valueOf(card, HudText.get("overlay.card.row.kills")));
        assertEquals("4", valueOf(card, HudText.get("overlay.card.row.threat")));
    }

    @Test
    void anArrivalWithNoKillsYetStillShowsTheCard() {
        resourceSite.droppedIn(HERE, ResourceSiteGrade.HIGH, 3);

        HudObjective card = cardHere().orElseThrow();

        assertEquals("0", valueOf(card, HudText.get("overlay.card.row.kills")),
                "the card is what tells the commander the app noticed where they are");
    }

    @Test
    void aSiteTheGameRatedHarmlessDropsTheThreatRow() {
        resourceSite.droppedIn(HERE, ResourceSiteGrade.LOW, 0);

        assertEquals(List.of(HudText.get("overlay.card.row.bounties"), HudText.get("overlay.card.row.kills")),
                HudCards.labels(cardHere().orElseThrow()),
                "a threat of zero is the game saying nothing, not saying safe");
    }

    @Test
    void aNavBeaconIsNotAHunt() {
        assertNull(ResourceSiteGrade.fromSymbol("$MULTIPLAYER_SCENARIO42_TITLE;"));

        resourceSite.droppedIn(HERE, ResourceSiteGrade.fromSymbol("$MULTIPLAYER_SCENARIO42_TITLE;"), 2);

        assertTrue(cardHere().isEmpty(), "only the four resource-site scenarios start a hunt");
    }

    @Test
    void theHuntSurvivesTheTripBetweenSites() {
        resourceSite.droppedIn(HERE, ResourceSiteGrade.HAZARDOUS, 4);
        claim(1, 250_000);

        // Boost out, cross the system in supercruise, drop at the station to look at the board. None
        // of that is leaving the hunt, and none of it may take the tally off the screen.
        assertTrue(cardHere().isPresent());

        resourceSite.droppedIn(HERE, ResourceSiteGrade.HIGH, 3);

        HudObjective card = cardHere().orElseThrow();
        assertEquals("ODOMAZOTZ - HIGH RES", card.subtitle(), "the card follows the commander to the next site");
        assertEquals(HudText.credits(250_000), valueOf(card, HudText.get("overlay.card.row.bounties")),
                "and the vouchers already earned are still theirs");
    }

    @Test
    void cashingInEndsTheHunt() {
        resourceSite.droppedIn(HERE, ResourceSiteGrade.HAZARDOUS, 4);
        claim(1, 250_000);

        resourceSite.cashedIn();

        assertTrue(cardHere().isEmpty(), "handing the vouchers over is the commander saying they are done");
    }

    /**
     * Redeeming with contracts still open marks the vouchers cashed in rather than deleting them, so
     * the massacre kill count keeps its evidence. The tally has to read that flag, or a commander who
     * cashed in mid-stack would go on being told they were carrying money they had already spent.
     */
    @Test
    void vouchersAlreadyCashedInAreNotStillInTheHold() {
        resourceSite.droppedIn(HERE, ResourceSiteGrade.HAZARDOUS, 4);
        claim(1, 250_000);
        bounties.markAllCashedIn();
        claim(2, 90_000);

        HudObjective card = cardHere().orElseThrow();

        assertEquals(HudText.credits(90_000), valueOf(card, HudText.get("overlay.card.row.bounties")));
        assertEquals("1", valueOf(card, HudText.get("overlay.card.row.kills")));
    }

    @Test
    void jumpingOutEndsTheHuntWithNoEventAtAll() {
        resourceSite.droppedIn(HERE, ResourceSiteGrade.HAZARDOUS, 4);

        assertTrue(card(SOMEWHERE_ELSE).isEmpty(),
                "a hunt belongs to the system it started in, so leaving ends it by arithmetic");
    }

    @Test
    void anOpenMassacreContractTakesTheCardBack() {
        resourceSite.droppedIn(HERE, ResourceSiteGrade.HAZARDOUS, 4);
        claim(1, 250_000);
        missions.save(GsonFactory.getGson().fromJson("""
                {"missionId":900,"missionType":"MISSION_PIRATE_MASSACRE","killCount":6,
                 "missionTargetFaction":"Odomazotz Silver Brotherhood","destinationSystem":"Odomazotz"}
                """, elite.intel.gameapi.journal.events.dto.MissionDto.class));

        assertTrue(cardHere().isEmpty(),
                "the massacre card says everything this one does and the kill count as well");

        missions.remove(900L);
    }

    @Test
    void aCommanderOutOfTheirShipIsNotHunting() {
        resourceSite.droppedIn(HERE, ResourceSiteGrade.HAZARDOUS, 4);

        assertTrue(card(HERE, false).isEmpty());
    }

    // -- fixtures --------------------------------------------------------------

    private Optional<HudObjective> cardHere() {
        return card(HERE);
    }

    private Optional<HudObjective> card(long systemAddress) {
        return card(systemAddress, true);
    }

    private Optional<HudObjective> card(long systemAddress, boolean inMainShip) {
        return new BountyHuntObjectiveSource(resourceSite, bounties, missions,
                () -> systemAddress, () -> "Odomazotz", () -> inMainShip).currentObjective();
    }

    private void claim(int index, long reward) {
        BountyDto bounty = new BountyDto();
        bounty.setPilotName("Pirate " + index);
        bounty.setTarget("viper_mkiv");
        bounty.setVictimFaction("Odomazotz Silver Brotherhood");
        bounty.setTotalReward(reward);
        bounty.setEarnedAt("2026-09-10T12:0" + index + ":00Z");
        bounties.add(bounty);
    }
}
