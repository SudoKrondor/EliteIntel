package elite.intel.ui.overlay;

import elite.intel.db.dao.RouteMonetisationDao;
import elite.intel.db.dao.RouteMonetisationDao.MonetisationTransaction;
import elite.intel.db.managers.BountyManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static elite.intel.ui.overlay.HudCards.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The rows these sources build are what the commander actually reads on the card, and the renderer
 * takes them as opaque data - so a wrong label or a value on the wrong row is invisible to every
 * other test in the suite.
 * <p>
 * Run against the real managers on the in-memory database rather than stubs, because these managers
 * are singletons with private constructors and because the card is only ever as correct as what the
 * tables actually hold.
 */
class ObjectiveSourceCardTest {

    private static final String TARGET = "Test Pirates";
    private static final String PROVIDER = "Test Providers";

    private final MissionManager missions = MissionManager.getInstance();
    private final BountyManager bounties = BountyManager.getInstance();
    private final MonetizeRouteManager monetized = MonetizeRouteManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private final List<Long> savedMissions = new ArrayList<>();
    private final List<BountyDto> savedBounties = new ArrayList<>();

    @BeforeEach
    void clean() {
        missions.clear();
        bounties.clear();
        monetized.clear();
    }

    @AfterEach
    void tidy() {
        savedMissions.forEach(missions::remove);
        savedBounties.forEach(bounties::remove);
        monetized.clear();
    }

    // -- missions --------------------------------------------------------------

    @Test
    void aCargoMissionShowsWhatToHaulAndWhereItGoes() {
        saveMission(missionJson(1, "MISSION_DELIVERY", """
                "commodityName":"Tritium","count":128,
                "destinationSystem":"Deciat","destinationStation":"Garay Terminal",
                "reward":2400000,"missionDescription":"Deliver Tritium"
                """));

        HudObjective card = new MissionObjectiveSource().currentObjective().orElseThrow();

        assertEquals("Deliver Tritium", card.title());
        assertEquals("Deciat - Garay Terminal", card.subtitle());
        assertEquals(List.of("CARGO", "REWARD"), labels(card));
        assertEquals("TRITIUM x128", valueOf(card, "CARGO"));
        assertEquals("2,400,000 cr", valueOf(card, "REWARD"));
    }

    @Test
    void aPassengerMissionCountsHeads() {
        saveMission(missionJson(2, "MISSION_SIGHTSEEING", """
                "passengerCount":8,"destinationSystem":"Sothis","reward":900000
                """));

        HudObjective card = new MissionObjectiveSource().currentObjective().orElseThrow();

        assertEquals(List.of("PASSENGERS", "REWARD"), labels(card));
        assertEquals("8", valueOf(card, "PASSENGERS"));
    }

    /**
     * Under six hours is the point where an expiry starts driving decisions, so it is coloured.
     */
    @Test
    void anExpiryInsideSixHoursIsFlagged() {
        saveMission(missionJson(3, "MISSION_DELIVERY", """
                "commodityName":"Gold","count":4,"reward":1000,
                "expiry":"%s"
                """.formatted(Instant.now().plusSeconds(3600).toString())));

        HudRow expiry = rowOf(new MissionObjectiveSource().currentObjective().orElseThrow(), "EXPIRES");

        assertEquals(HudRow.State.WARN, expiry.state());
    }

    @Test
    void anExpiredMissionSaysSoRatherThanCountingBackwards() {
        saveMission(missionJson(4, "MISSION_DELIVERY", """
                "commodityName":"Gold","count":4,"reward":1000,
                "expiry":"%s"
                """.formatted(Instant.now().minusSeconds(60).toString())));

        HudObjective card = new MissionObjectiveSource().currentObjective().orElseThrow();

        assertEquals(HudRow.State.CRITICAL, rowOf(card, "EXPIRED").state());
    }

    @Test
    void noMissionsMeansNoCard() {
        assertTrue(new MissionObjectiveSource().currentObjective().isEmpty());
    }

    // -- massacre stack --------------------------------------------------------

    @Test
    void aMassacreStackDrawsOneSharedKillBar() {
        // Two different providers: their missions run at the same time and share every kill.
        saveMission(massacreJson(10, 12, PROVIDER));
        saveMission(massacreJson(11, 8, "Second " + PROVIDER));
        saveBounties(5);

        HudObjective card = new MassacreObjectiveSource().currentObjective().orElseThrow();

        assertEquals("MASSACRE CONTRACT", card.title());
        assertEquals(TARGET.toUpperCase(), card.subtitle());
        assertEquals(HudObjective.PRIORITY_SPECIALISED, card.priority(),
                "it must outrank the generic mission card it replaces");

        HudRow bar = rowOf(card, "PIRATES (EST)");
        assertTrue(bar.hasProgress());
        assertEquals(5, bar.current(), "one kill advances every mission in the stack");
        assertEquals(12, bar.max(), "the stack costs its longest chain, not the sum");
        assertEquals("2", valueOf(card, "MISSIONS"));
    }

    /**
     * Kills are inferred from bounty vouchers, which over-count - the journal never says who landed
     * the final blow. The bar has to admit that rather than present a guess as a count, right up
     * until the game's own redirect makes it exact.
     */
    @Test
    void anUnconfirmedBarIsLabelledAnEstimate() {
        saveMission(massacreJson(16, 6));
        saveBounties(2);

        HudObjective card = new MassacreObjectiveSource().currentObjective().orElseThrow();

        assertTrue(labels(card).contains("PIRATES (EST)"), labels(card).toString());
        assertFalse(labels(card).contains("PIRATES"), "an estimate must not read as a count");
    }

    @Test
    void aConfirmedBarDropsTheEstimateMarker() {
        saveMission(redirectedMassacreJson(17, 6));

        HudObjective card = new MassacreObjectiveSource().currentObjective().orElseThrow();

        assertTrue(labels(card).contains("PIRATES"), labels(card).toString());
        assertFalse(labels(card).contains("PIRATES (EST)"), "the game confirmed it, so it is not a guess");
    }

    @Test
    void aStackTheGameConfirmedTurnsTheBarGood() {
        saveMission(redirectedMassacreJson(12, 3));
        saveBounties(3);

        HudRow bar = rowOf(new MassacreObjectiveSource().currentObjective().orElseThrow(), "PIRATES");

        assertEquals(HudRow.State.GOOD, bar.state());
        assertEquals(bar.max(), bar.current());
    }

    /**
     * The bug: twelve bounties against a twelve-kill contract drew a full green bar while the game
     * still wanted two more kills. A bounty is not proof of mission credit, so the bar holds one
     * short of full until the game redirects the mission to its turn-in point.
     */
    @Test
    void killsAloneNeverFillTheBar() {
        saveMission(massacreJson(15, 3));
        saveBounties(5);

        HudRow bar = rowOf(new MassacreObjectiveSource().currentObjective().orElseThrow(), "PIRATES (EST)");

        assertEquals(HudRow.State.NORMAL, bar.state());
        assertEquals(2, bar.current());
        assertEquals(3, bar.max());
    }

    /**
     * A single mission is described perfectly well by the generic card, so no MISSIONS row.
     */
    @Test
    void aLoneMassacreMissionDoesNotClaimAStack() {
        saveMission(massacreJson(13, 6));

        HudObjective card = new MassacreObjectiveSource().currentObjective().orElseThrow();

        assertTrue(labels(card).stream().noneMatch("MISSIONS"::equals), labels(card).toString());
    }

    @Test
    void noPirateMissionsMeansNoMassacreCard() {
        saveMission(missionJson(14, "MISSION_DELIVERY", "\"commodityName\":\"Gold\",\"count\":1,\"reward\":10"));

        assertTrue(new MassacreObjectiveSource().currentObjective().isEmpty());
    }

    // -- monetised route -------------------------------------------------------

    @Test
    void aCargoOpportunityShowsBothEndsAndTheMargin() {
        saveMonetizedRoute();

        HudObjective card = new MonetizedRouteObjectiveSource().currentObjective().orElseThrow();

        assertEquals("CARGO OPPORTUNITY", card.title());
        assertEquals(List.of("COMMODITY", "BUY", "SELL", "MARGIN"), labels(card));
        assertEquals("GOLD", valueOf(card, "COMMODITY"));
        assertEquals("SOL - ABRAHAM LINCOLN", valueOf(card, "BUY"));
        assertEquals("DECIAT - GARAY TERMINAL", valueOf(card, "SELL"));
        assertEquals("3,000 cr/t", valueOf(card, "MARGIN"));
        assertEquals(HudRow.State.GOOD, rowOf(card, "SELL").state(), "the sell leg is the payoff");
    }

    /**
     * MarketSellEventSubscriber clears the transaction once the cargo is sold, so the card has to
     * disappear on its own. There is no other clear path.
     */
    @Test
    void sellingTheCargoRemovesTheCard() {
        saveMonetizedRoute();
        assertTrue(new MonetizedRouteObjectiveSource().currentObjective().isPresent());

        monetized.clear();

        assertTrue(new MonetizedRouteObjectiveSource().currentObjective().isEmpty());
    }

    // -- fixtures --------------------------------------------------------------

    private void saveMission(String json) {
        MissionDto mission = GsonFactory.getGson().fromJson(json, MissionDto.class);
        missions.save(mission);
        savedMissions.add(mission.getMissionId());
    }

    private String missionJson(long id, String type, String fields) {
        return "{\"missionId\":%d,\"missionType\":\"%s\",\"faction\":\"%s\",\"acceptedAt\":\"%s\",%s}"
                .formatted(id, type, PROVIDER, Instant.now().minusSeconds(7200).toString(), fields.strip());
    }

    private String massacreJson(long id, int kills) {
        return massacreJson(id, kills, PROVIDER);
    }

    /**
     * A massacre mission the game has redirected to its turn-in point, i.e. one it has confirmed
     * the kills for - the only thing that completes a kill contract.
     */
    private String redirectedMassacreJson(long id, int kills) {
        return massacreJson(id, kills).replaceFirst("}$",
                ",\"redirectedAt\":\"%s\"}".formatted(Instant.now().minusSeconds(300).toString()));
    }

    private String massacreJson(long id, int kills, String provider) {
        return "{\"missionId\":%d,\"missionType\":\"MISSION_PIRATE_MASSACRE\",\"faction\":\"%s\","
                .formatted(id, provider)
                + "\"acceptedAt\":\"%s\",".formatted(Instant.now().minusSeconds(7200).toString())
                + "\"killCount\":%d,\"missionTargetFaction\":\"%s\",\"reward\":1000000}".formatted(kills, TARGET);
    }

    /**
     * Kills earned after acceptance, which is what makes them count toward the stack.
     */
    private void saveBounties(int count) {
        Instant base = Instant.now().minusSeconds(600);
        for (int i = 0; i < count; i++) {
            BountyDto bounty = new BountyDto();
            bounty.setVictimFaction(TARGET);
            bounty.setPilotName("test-pilot-" + i);
            bounty.setEarnedAt(base.plusSeconds(i).toString());
            bounty.setRewards(List.of());
            bounties.add(bounty);
            savedBounties.add(bounty);
        }
        // The source reads bounties through the session, so this asserts the wiring too.
        assertEquals(count, playerSession.getBounties().size());
    }

    private void saveMonetizedRoute() {
        MonetisationTransaction tx = new MonetisationTransaction();
        tx.setSourceCommodity("Gold");
        tx.setSourceStarSystem("Sol");
        tx.setSourceStationName("Abraham Lincoln");
        tx.setSourceStationType("Orbis");
        tx.setSourceBuyPrice(5000);
        tx.setSourceSupply(1000);
        tx.setDestinationCommodity("Gold");
        tx.setDestinationStarSystem("Deciat");
        tx.setDestinationStationName("Garay Terminal");
        tx.setDestinationStationType("Coriolis");
        tx.setDestinationSellPrice(8000);
        tx.setDestinationDemand(500);
        Database.withDao(RouteMonetisationDao.class, dao -> {
            dao.save(tx);
            return null;
        });
    }

}
