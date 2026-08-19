package elite.intel.gameapi.missions;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which mission the app plots a route to when the commander is holding several.
 * <p>
 * The fixtures are the board from the bug report, copied out of the journal verbatim: two supply runs
 * and a delivery with real destinations, two donations with none, and two kill contracts. Asking to
 * navigate to the active mission picked a donation, and a donation has no {@code DestinationSystem} at
 * all - the game completes it at the board it was accepted at - so the galaxy map was handed a null
 * system name and the route was silently never plotted.
 */
class MissionSelectionTest {

    private static final MissionDto SUPPLY_WEAPONS = accepted("""
            {"timestamp":"2026-08-19T17:38:30Z","event":"MissionAccepted","Name":"Mission_Delivery",
             "LocalisedName":"Supply 45 units of Non-Lethal Weapons","Faction":"Cadubii Partners",
             "MissionID":1063795660,"DestinationSystem":"Niu Lang O","DestinationStation":"Hirasawa Station"}
            """);
    private static final MissionDto SUPPLY_ARMOUR = accepted("""
            {"timestamp":"2026-08-19T17:38:40Z","event":"MissionAccepted","Name":"Mission_Delivery",
             "LocalisedName":"Supply 24 units of Reactive Armour","Faction":"Cadubii Partners",
             "MissionID":1063795687,"DestinationSystem":"Niu Lang O","DestinationStation":"Hirasawa Station"}
            """);
    private static final MissionDto LIQUOR = accepted("""
            {"timestamp":"2026-08-19T18:05:58Z","event":"MissionAccepted","Name":"Mission_Delivery",
             "LocalisedName":"Deliver 196 units of Liquor","Faction":"Cadubii Partners",
             "MissionID":1063798079,"DestinationSystem":"Col 285 Sector WF-E c12-13",
             "DestinationStation":"Bachman Mine"}
            """);
    /**
     * A donation: no destination of any kind, which is the whole point of the fixture.
     */
    private static final MissionDto DONATION_575K = accepted("""
            {"timestamp":"2026-08-19T18:57:11Z","event":"MissionAccepted","Name":"Mission_AltruismCredits",
             "LocalisedName":"Donate 575,000 Cr to the cause","Faction":"6th Interstellar Corps",
             "MissionID":1063802185}
            """);
    private static final MissionDto DONATION_750K = accepted("""
            {"timestamp":"2026-08-19T18:57:16Z","event":"MissionAccepted","Name":"Mission_AltruismCredits",
             "LocalisedName":"Donate 750,000 Cr to the cause","Faction":"6th Interstellar Corps",
             "MissionID":1063802193}
            """);
    private static final MissionDto MASSACRE_LUDWIG = accepted("""
            {"timestamp":"2026-08-19T18:58:12Z","event":"MissionAccepted","Name":"Mission_Massacre",
             "LocalisedName":"Kill Xue Davokje Blue Cartel faction Pirates","Faction":"Cadubii Partners",
             "MissionID":1063802251,"KillCount":7,"DestinationSystem":"Xue Davokje",
             "DestinationStation":"Ludwig Struve Gateway"}
            """);
    private static final MissionDto MASSACRE_SEAMANS = accepted("""
            {"timestamp":"2026-08-19T18:58:27Z","event":"MissionAccepted","Name":"Mission_Massacre",
             "LocalisedName":"Kill Xue Davokje Blue Cartel faction Pirates","Faction":"Cadubii Partners",
             "MissionID":1063802270,"KillCount":12,"DestinationSystem":"Xue Davokje",
             "DestinationStation":"Seamans Hub"}
            """);

    /**
     * The board exactly as the commander was holding it, donations in the middle of the stack.
     */
    private static final List<MissionDto> BOARD = List.of(
            SUPPLY_WEAPONS, SUPPLY_ARMOUR, DONATION_575K, DONATION_750K,
            LIQUOR, MASSACRE_LUDWIG, MASSACRE_SEAMANS);

    /**
     * The commander was docked at Maiga, where the donations were taken.
     */
    private static final String DOCKED_AT = "Maiga";

    // -- the reported bug ------------------------------------------------------

    @Test
    @DisplayName("a donation is never what the app plots a route to")
    void aStackWithDonationsInItStillPlotsSomewhere() {
        MissionDto chosen = MissionSelection.toPlotFor(BOARD, DOCKED_AT).orElseThrow();

        assertNotNull(chosen.getDestinationSystem(), "handing the galaxy map a null is the bug");
        assertEquals("Xue Davokje", chosen.getDestinationSystem());
        assertEquals("Seamans Hub", chosen.getDestinationStation(),
                "the newest mission that has anywhere to go");
    }

    @Test
    void donationsAloneHaveNowhereToFlyTo() {
        assertTrue(MissionSelection.toPlotFor(List.of(DONATION_575K, DONATION_750K), DOCKED_AT).isEmpty(),
                "which is a different answer to having no missions, and has to be said out loud");
    }

    @Test
    void anEmptyBoardSelectsNothing() {
        assertTrue(MissionSelection.toPlotFor(List.of(), DOCKED_AT).isEmpty());
    }

    // -- ordering --------------------------------------------------------------

    @Test
    @DisplayName("the newest mission with a destination wins, matching the game's own list order")
    void theNewestPlottableMissionIsChosen() {
        List<MissionDto> deliveriesOnly = List.of(SUPPLY_WEAPONS, LIQUOR, SUPPLY_ARMOUR);

        assertEquals("Col 285 Sector WF-E c12-13",
                MissionSelection.toPlotFor(deliveriesOnly, DOCKED_AT).orElseThrow().getDestinationSystem());
    }

    /**
     * The order the board is handed over in is a map's, so the answer must not depend on it or the
     * command plots somewhere different each time it is asked.
     */
    @Test
    void theAnswerDoesNotDependOnTheOrderTheBoardArrivesIn() {
        Optional<MissionDto> forwards = MissionSelection.toPlotFor(BOARD, DOCKED_AT);
        Optional<MissionDto> backwards = MissionSelection.toPlotFor(BOARD.reversed(), DOCKED_AT);

        assertEquals(forwards.orElseThrow().getMissionId(), backwards.orElseThrow().getMissionId());
    }

    /**
     * Plotting to the system the ship is parked in opens the galaxy map, types a name and produces no
     * route at all - so a mission that goes somewhere is worth more than a newer one that does not.
     */
    @Test
    void aMissionSomewhereElseBeatsANewerOneEndingWhereTheShipAlreadyIs() {
        MissionDto chosen = MissionSelection.toPlotFor(
                List.of(SUPPLY_WEAPONS, MASSACRE_SEAMANS), "Xue Davokje").orElseThrow();

        assertEquals("Niu Lang O", chosen.getDestinationSystem());
    }

    @Test
    void aBoardEndingOnlyWhereTheShipIsIsStillPlottable() {
        assertEquals("Xue Davokje",
                MissionSelection.toPlotFor(List.of(MASSACRE_SEAMANS), "Xue Davokje")
                        .orElseThrow().getDestinationSystem());
    }

    @Test
    void anUnknownPositionDoesNotStopTheSelection() {
        assertEquals("Xue Davokje",
                MissionSelection.toPlotFor(BOARD, null).orElseThrow().getDestinationSystem());
    }

    // -- destinations ----------------------------------------------------------

    @Test
    void aBlankDestinationCountsAsNoDestination() {
        MissionDto blank = accepted("""
                {"timestamp":"2026-08-19T19:00:00Z","event":"MissionAccepted","Name":"Mission_Delivery",
                 "LocalisedName":"Blank","Faction":"Test","MissionID":1,"DestinationSystem":"  "}
                """);

        assertFalse(MissionSelection.hasDestination(blank));
        assertTrue(MissionSelection.toPlotFor(List.of(blank), DOCKED_AT).isEmpty());
    }

    @Test
    void aNullInTheStackIsSteppedOver() {
        List<MissionDto> withHole = java.util.Arrays.asList(null, LIQUOR);

        assertEquals("Col 285 Sector WF-E c12-13",
                MissionSelection.toPlotFor(withHole, DOCKED_AT).orElseThrow().getDestinationSystem());
    }

    private static MissionDto accepted(String journalLine) {
        return new MissionDto(new MissionAcceptedEvent(
                GsonFactory.getGson().fromJson(journalLine, JsonObject.class)));
    }
}
