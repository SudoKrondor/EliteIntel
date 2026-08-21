package elite.intel.ui.overlay;

import com.google.gson.JsonObject;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static elite.intel.ui.overlay.HudCards.labels;
import static elite.intel.ui.overlay.HudCards.valueOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A stacked board is the normal case, not the edge case - a commander picks up eight couriers at
 * once - and the card can only show one of them. These tests pin which one, because getting it
 * wrong is invisible to every other test: the card still renders, it just names a system the
 * commander is not flying to.
 * <p>
 * The fixtures are the real thing. {@link #COURIER_STACK} is ten Internal Report Delivery missions
 * copied from a live journal, mission IDs included, because the bug that prompted this was a
 * {@code HashMap} iteration artefact and a fixture using 1..10 would not reproduce it.
 */
class MissionStackCardTest {

    /**
     * Ten couriers accepted over seven minutes from one board. In the game's own list the top entry
     * was G 173-53 / Wedge's Folly, the last one accepted; the overlay was showing LP 149-14.
     */
    private static final List<MissionDto> COURIER_STACK = List.of(
            courier(1062786850, "G 190-28", "Townshend Enterprise", "02:57:34", 35744),
            courier(1062786863, "V439 Andromedae", "Baxter Settlement", "02:57:45", 19678),
            courier(1062787198, "Core Sys Sector QD-T a3-5", "Gilling Vision", "03:03:25", 48720),
            courier(1062787203, "LFT 6", "Lamarck Colony", "03:03:30", 29681),
            courier(1062787209, "LP 149-14", "Caidin City", "03:03:33", 93872),
            courier(1062787227, "LP 245-10", "Galton Gateway", "03:03:40", 47554),
            courier(1062787238, "LP 245-10", "Wellman Hub", "03:03:53", 47565),
            courier(1062787247, "Piscium Sector ZF-N a7-4", "Borrelly Relay", "03:03:59", 49671),
            courier(1062787252, "Umpilakap", "Bushnell Refinery", "03:04:05", 57432),
            courier(1062787261, "G 173-53", "Wedge's Folly", "03:04:09", 53802));

    private final MissionManager missions = MissionManager.getInstance();
    private final ShipRouteManager shipRoute = ShipRouteManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    private final List<Long> saved = new ArrayList<>();
    private String previousPosition;

    private String route;
    private String here;

    @BeforeEach
    void clean() {
        missions.clear();
        shipRoute.clearRoute();
        previousPosition = playerSession.getPrimaryStarName();
        route = null;
        here = null;
    }

    @AfterEach
    void tidy() {
        saved.forEach(missions::remove);
        shipRoute.clearRoute();
        playerSession.setCurrentPrimaryStarName(previousPosition);
    }

    // -- which mission is featured ---------------------------------------------

    /**
     * The regression this class exists for. With nothing populating the expiry field the old card
     * sorted a stack of ties and took whatever the map handed back first, which for these exact ten
     * mission IDs is LP 149-14 - which is what was on screen while the game listed G 173-53.
     */
    @Test
    void withNoRouteAndNoDeadlinesTheTopOfTheGameListIsShown() {
        assertEquals("G 173-53", featured(null).getDestinationSystem(),
                "nothing in this stack expires, so the newest accepted mission - the game's own first "
                        + "entry - still decides it");
    }

    @Test
    void aPlottedRouteDecidesWhichMissionIsShown() {
        MissionDto shown = featured("Umpilakap");

        assertEquals("Umpilakap", shown.getDestinationSystem());
        assertEquals("Bushnell Refinery", shown.getDestinationStation());
    }

    @Test
    void aRouteToSomewhereWithNoMissionFallsBackToTheListOrder() {
        assertEquals("G 173-53", featured("Deciat").getDestinationSystem());
    }

    /**
     * With no route there is nothing stating where the ship is going, so the deadline decides - the
     * mission running out first is the one worth looking at, whatever order the board was taken in.
     */
    @Test
    void withNoRouteTheSoonestDeadlineIsShown() {
        List<MissionDto> stack = List.of(
                expiring(1, "Deciat", "2026-08-09T12:00:00Z"),
                expiring(2, "Sol", "2026-08-09T04:00:00Z"),
                expiring(3, "Shinrarta Dezhra", "2026-08-10T00:00:00Z"));

        assertEquals("Sol", MissionObjectiveSource.featured(stack, null).orElseThrow().getDestinationSystem());
    }

    /**
     * A mission that never expires does not outrank one that does, however recently it was taken.
     */
    @Test
    void aDeadlineOutranksAMissionWithoutOne() {
        List<MissionDto> stack = List.of(
                courier(9, "Deciat", "Broglie Terminal", "23:59:59", 1000),
                expiring(1, "Sol", "2026-08-10T00:00:00Z"));

        assertEquals("Sol", MissionObjectiveSource.featured(stack, null).orElseThrow().getDestinationSystem());
    }

    /**
     * The route is the commander saying where they are going, so it beats the deadline: flying to one
     * drop while the card counts down another is how the wrong cargo gets handed over.
     */
    @Test
    void aPlottedRouteOutranksTheSoonestDeadline() {
        List<MissionDto> stack = List.of(
                expiring(1, "Deciat", "2026-08-09T12:00:00Z"),
                expiring(2, "Sol", "2026-08-09T04:00:00Z"));

        assertEquals("Deciat",
                MissionObjectiveSource.featured(stack, "Deciat").orElseThrow().getDestinationSystem());
    }

    /**
     * Two missions end at LP 245-10. Whichever is featured, it has to be the same one on every
     * poll, or the card rewrites itself once a second.
     */
    @Test
    void tiesInsideASystemResolveTheSameWayEveryTime() {
        Optional<MissionDto> first = MissionObjectiveSource.featured(COURIER_STACK, "LP 245-10");
        Optional<MissionDto> again = MissionObjectiveSource.featured(reversed(), "lp 245-10");

        assertEquals(first.orElseThrow().getMissionId(), again.orElseThrow().getMissionId());
        assertEquals("Wellman Hub", first.orElseThrow().getDestinationStation(),
                "the later-accepted of the two sits higher in the list");
    }

    @Test
    void noMissionsMeansNoCard() {
        assertTrue(MissionObjectiveSource.featured(List.of(), "Sol").isEmpty());
        assertTrue(source().currentObjective().isEmpty());
    }

    // -- wiring ----------------------------------------------------------------

    /**
     * Everything above drives the selection through the test seam, which would still pass if the
     * supplier on the real constructor read the wrong manager. This one plots an actual route, so the
     * wiring itself is on the hook.
     * <p>
     * The ship is deliberately parked in a system one of the missions ends at: where the ship IS was
     * once a selection tier of its own, and is not one any more.
     */
    @Test
    void theRealCardReadsTheRouteItIsWiredTo() {
        COURIER_STACK.forEach(this::save);
        plotRoute("LHS 3447", "Umpilakap");
        playerSession.setCurrentPrimaryStarName("LFT 6");

        HudObjective card = new MissionObjectiveSource().currentObjective().orElseThrow();

        assertEquals("Umpilakap - Bushnell Refinery", card.subtitle(),
                "the plotted route's final leg picks the mission");

        shipRoute.clearRoute();
        card = new MissionObjectiveSource().currentObjective().orElseThrow();

        assertEquals("G 173-53 - Wedge's Folly", card.subtitle(),
                "with the route gone it falls to the expiry order, not to the system the ship sits in");
    }

    // -- what the card says about the rest of the stack -------------------------

    @Test
    void theCardCountsTheStackAndWhatItIsWorth() {
        COURIER_STACK.forEach(this::save);

        HudObjective card = source().currentObjective().orElseThrow();

        assertEquals("G 173-53 - Wedge's Folly", card.subtitle());
        assertEquals("53,802 cr", valueOf(card, "REWARD"), "the featured mission's own payout");
        assertEquals("10 MISSIONS", valueOf(card, "STACK"));
        assertEquals("483,719 cr", valueOf(card, "STACK REWARD"), "what finishing the board pays");
    }

    /**
     * Galton Gateway and Wellman Hub are one trip, and the card has to say so - it is the whole
     * reason to fly there next.
     */
    @Test
    void twoMissionsToOneSystemAreFlaggedAsOneTrip() {
        COURIER_STACK.forEach(this::save);
        route = "LP 245-10";

        assertEquals("2 MISSIONS", valueOf(source().currentObjective().orElseThrow(), "SAME SYSTEM"));
    }

    @Test
    void oneMissionPerSystemSaysNothingAboutSharingIt() {
        COURIER_STACK.forEach(this::save);

        assertFalse(labels(source().currentObjective().orElseThrow()).contains("SAME SYSTEM"));
    }

    @Test
    void aLoneMissionDoesNotClaimAStack() {
        save(COURIER_STACK.getFirst());

        List<String> labels = labels(source().currentObjective().orElseThrow());

        assertFalse(labels.contains("STACK"), labels.toString());
        assertFalse(labels.contains("STACK REWARD"), labels.toString());
    }

    /**
     * The stack is the family, not the mission board: a massacre contract taken on the side is not
     * part of a courier run and must not inflate what the run is worth.
     */
    @Test
    void anUnrelatedMissionIsNotPartOfTheStack() {
        COURIER_STACK.forEach(this::save);
        save(GsonFactory.getGson().fromJson("""
                {"missionId":900,"missionType":"MISSION_PIRATE_MASSACRE","acceptedAt":"2026-08-08T01:00:00Z",
                 "killCount":20,"reward":5000000,"destinationSystem":"LHS 1050"}
                """, MissionDto.class));

        assertEquals("10 MISSIONS", valueOf(source().currentObjective().orElseThrow(), "STACK"));
    }

    // -- expiry ----------------------------------------------------------------

    /**
     * Nothing carried Expiry from the accepted event onto the stored mission, so the EXPIRES row
     * could never render and every mission tied when the stack was sorted on it.
     */
    @Test
    void anAcceptedMissionRemembersWhenItExpires() {
        JsonObject json = GsonFactory.getGson().fromJson("""
                {"timestamp":"2026-08-08T03:04:09Z","event":"MissionAccepted","Name":"Mission_Courier_Democracy",
                 "LocalisedName":"Internal report delivery","Faction":"Test Faction","MissionID":1062787261,
                 "DestinationSystem":"G 173-53","DestinationStation":"Wedge's Folly",
                 "Expiry":"2026-08-09T03:03:10Z","Reward":53802}
                """, JsonObject.class);

        MissionDto mission = new MissionDto(new MissionAcceptedEvent(json));

        assertEquals("2026-08-09T03:03:10Z", mission.getExpiry());
    }

    // -- fixtures --------------------------------------------------------------

    private MissionObjectiveSource source() {
        return new MissionObjectiveSource(missions, () -> route);
    }

    private MissionDto featured(String routeDestination) {
        return MissionObjectiveSource.featured(COURIER_STACK, routeDestination).orElseThrow();
    }

    /**
     * A courier with a deadline. The stack above carries none - it predates expiry being recorded -
     * which is exactly the case the expiry order has to fall back out of.
     */
    private static MissionDto expiring(long id, String system, String expiry) {
        MissionDto mission = courier(id, system, "Some Port", "03:00:00", 1000);
        mission.setExpiry(expiry);
        return mission;
    }

    private List<MissionDto> reversed() {
        List<MissionDto> copy = new ArrayList<>(COURIER_STACK);
        Collections.reverse(copy);
        return copy;
    }

    private void plotRoute(String... systems) {
        for (int i = 0; i < systems.length; i++) {
            NavRouteDto leg = new NavRouteDto();
            leg.setLeg(i + 1);
            leg.setName(systems[i]);
            leg.setRemainingJumps(systems.length - i - 1);
            leg.setStarClass("G");
            shipRoute.updateRouteNode(leg);
        }
    }

    private void save(MissionDto mission) {
        missions.save(mission);
        saved.add(mission.getMissionId());
    }

    private static MissionDto courier(long id, String system, String station, String acceptedAt, long reward) {
        return GsonFactory.getGson().fromJson("""
                {"missionId":%d,"missionType":"MISSION_COURIER","faction":"Test Faction",
                 "missionDescription":"Internal report delivery","acceptedAt":"2026-08-08T%sZ",
                 "destinationSystem":"%s","destinationStation":"%s","reward":%d}
                """.formatted(id, acceptedAt, system, station, reward), MissionDto.class);
    }
}
