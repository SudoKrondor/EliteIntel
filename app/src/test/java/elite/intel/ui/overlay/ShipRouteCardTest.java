package elite.intel.ui.overlay;

import elite.intel.db.dao.DestinationReminderDao.Reminder;
import elite.intel.gameapi.ReminderContact;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static elite.intel.ui.overlay.HudCards.labels;
import static elite.intel.ui.overlay.HudCards.valueOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * With no mission and nothing else to report, the card says where the commander is pointed.
 */
class ShipRouteCardTest {

    @AfterEach
    void restoreLanguage() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void nothingPlottedShowsNoCard() {
        assertTrue(source(null).currentObjective().isEmpty());
        assertTrue(source("   ").currentObjective().isEmpty());
    }

    @Test
    void aPlottedRouteNamesTheDestinationAndWhatIsLeft() {
        HudObjective card = source("Colonia", "HIP 12345", "Eagle Sector", "Colonia")
                .currentObjective().orElseThrow();

        assertEquals("PLOTTED ROUTE", card.title());
        assertEquals("COLONIA", valueOf(card, "DESTINATION"));
        assertEquals("HIP 12345", valueOf(card, "NEXT"));
        assertEquals("3", valueOf(card, "JUMPS"));
        assertEquals(List.of("DESTINATION", "NEXT", "JUMPS"), labels(card), "destination reads first");
    }

    /**
     * The jump count is the one the app already speaks on arrival ("N jumps left"), taken from the same list,
     * so the card and the voice cannot disagree about the same route.
     */
    @Test
    void theJumpCountIsTheNumberOfLegsStillAhead() {
        assertEquals("1", valueOf(source("Sol", "Sol").currentObjective().orElseThrow(), "JUMPS"));
        assertEquals("5", valueOf(
                source("Sol", "A", "B", "C", "D", "Sol").currentObjective().orElseThrow(), "JUMPS"));
    }

    /**
     * On the last hop the next system is the destination; the destination row already says so.
     */
    @Test
    void theLastHopDoesNotRepeatTheDestination() {
        HudObjective card = source("Sol", "Sol").currentObjective().orElseThrow();

        assertFalse(labels(card).contains("NEXT"));
        assertEquals(List.of("DESTINATION", "JUMPS"), labels(card));
    }

    /**
     * The destination is a labelled row, not a subtitle - one place, plainly named.
     */
    @Test
    void theDestinationIsNotAlsoASubtitle() {
        HudObjective card = source("Colonia", "HIP 12345").currentObjective().orElseThrow();

        assertNull(card.subtitle());
        assertEquals("COLONIA", valueOf(card, "DESTINATION"));
    }

    @Test
    void aDestinationWithNoLegsLeftStillNamesIt() {
        // The route table can be empty while the destination is still known (the last leg just cleared).
        HudObjective card = source("Colonia").currentObjective().orElseThrow();

        assertEquals(List.of("DESTINATION"), labels(card), "nothing to count and nowhere further to name");
        assertEquals("COLONIA", valueOf(card, "DESTINATION"));
    }

    @Test
    void nextWaypointIgnoresABlankOrDuplicateName() {
        assertEquals(Optional.empty(), ShipRouteObjectiveSource.nextWaypoint(List.of(), "Sol"));
        assertEquals(Optional.empty(), ShipRouteObjectiveSource.nextWaypoint(null, "Sol"));
        assertEquals(Optional.empty(), ShipRouteObjectiveSource.nextWaypoint(legs("  "), "Sol"));
        assertEquals(Optional.of("Alpha"), ShipRouteObjectiveSource.nextWaypoint(legs("Alpha"), "Sol"));
    }

    /**
     * Ranked below everything the commander took on: a mission, a trade route or a standing errand all say
     * why they are flying somewhere, and this only says where.
     */
    @Test
    void aMissionTakesTheCard() {
        Optional<HudObjective> shown = NativeHudOverlay.highestPriority(List.of(
                source("Colonia", "HIP 12345"),
                () -> Optional.of(new HudObjective("mission", "MISSION", null, List.of(),
                        HudObjective.PRIORITY_STANDING))));

        assertEquals("mission", shown.orElseThrow().id());
    }

    @Test
    void withNoMissionTheRouteIsTheCard() {
        Optional<HudObjective> shown = NativeHudOverlay.highestPriority(List.of(
                Optional::empty,
                source("Colonia", "HIP 12345")));

        assertEquals("ship-route", shown.orElseThrow().id());
    }

    // ── a destination the app worked out enriches the route ──────────────────

    /**
     * When the app plotted the route itself it also recorded the port and the contact, and that detail is
     * worth having: "material trader at Jameson Memorial" beats a bare system name.
     */
    @Test
    void anErrandAtTheDestinationAddsThePortAndTheContact() {
        HudObjective card = sourceWithErrand("Shinrarta Dezhra", "Shinrarta Dezhra", "Jameson Memorial",
                ReminderContact.MATERIAL_TRADER_MANUFACTURED, "HIP 12345", "Shinrarta Dezhra")
                .currentObjective().orElseThrow();

        assertEquals("MATERIAL TRADER", card.title());
        assertEquals("SHINRARTA DEZHRA", valueOf(card, "DESTINATION"));
        assertEquals("JAMESON MEMORIAL", valueOf(card, "STATION"));
        assertEquals("MANUFACTURED", valueOf(card, "TYPE"));
        assertEquals("2", valueOf(card, "JUMPS"));
    }

    /**
     * The reported failure, twice: a route set to Groombridge 34 while a Sterope II trade errand from an
     * earlier journey still sat in the database. A reminder is cleared only on request, so it must never
     * describe a route that does not go there.
     */
    @Test
    void anErrandSomewhereElseIsIgnoredEntirely() {
        HudObjective card = sourceWithErrand("Groombridge 34", "Sterope II", "Onisun Arboretum", null,
                "Aries Dark Region RJ-P b6-3", "Groombridge 34")
                .currentObjective().orElseThrow();

        assertEquals("PLOTTED ROUTE", card.title());
        assertEquals("GROOMBRIDGE 34", valueOf(card, "DESTINATION"));
        assertEquals(List.of("DESTINATION", "NEXT", "JUMPS"), labels(card),
                "a stale errand contributes nothing");
    }

    @Test
    void interstellarFactorsHasNoTypeToReport() {
        HudObjective card = sourceWithErrand("Sol", "Sol", "Abraham Lincoln",
                ReminderContact.INTERSTELLAR_FACTORS, "Sol").currentObjective().orElseThrow();

        assertEquals("INTERSTELLAR FACTORS", card.title());
        assertFalse(labels(card).contains("TYPE"), "no flavour of interstellar factors to name");
        assertEquals("ABRAHAM LINCOLN", valueOf(card, "STATION"));
    }

    /**
     * A contact written by a newer build, or edited by hand, must not cost the whole card.
     */
    @Test
    void anUnrecognisedContactFallsBackRatherThanFailing() {
        Reminder stored = new Reminder();
        stored.setStarSystem("Sol");
        stored.setStationName("Abraham Lincoln");
        stored.setContact("SOMETHING_A_LATER_BUILD_ADDED");

        HudObjective card = new ShipRouteObjectiveSource(() -> "Sol", () -> legs("Sol"), () -> stored)
                .currentObjective().orElseThrow();

        assertEquals("PLOTTED ROUTE", card.title());
        assertEquals("ABRAHAM LINCOLN", valueOf(card, "STATION"));
    }

    /**
     * The stored sentence is prose written for the voice, in whatever language it was created in. It is never
     * a row value: a card row is a label and a short value, and this is neither.
     */
    @Test
    void theSpokenSentenceIsNeverDrawn() {
        HudObjective card = sourceWithErrand("Sol", "Sol", "Abraham Lincoln",
                ReminderContact.MATERIAL_TRADER_RAW, "Sol").currentObjective().orElseThrow();

        assertFalse(card.rows().stream().anyMatch(
                row -> "some sentence written for the voice".equals(row.value())));
        assertNull(card.subtitle());
    }

    @Test
    void theCardSpeaksTheCommandersLanguage() {
        SystemSession.getInstance().setLanguage(Language.DE);

        HudObjective card = source("Colonia", "HIP 12345", "Colonia").currentObjective().orElseThrow();

        assertEquals("GEPLANTE ROUTE", card.title());
        assertEquals("2", valueOf(card, "SPRÜNGE"));
        // The label is ours and is translated; the system name came from the game and is not.
        assertEquals("COLONIA", valueOf(card, "ZIEL"));
    }

    private static ShipRouteObjectiveSource source(String destination, String... legs) {
        return new ShipRouteObjectiveSource(() -> destination, () -> legs(legs), () -> null);
    }

    /**
     * A route with a standing errand recorded against {@code errandSystem}.
     */
    private static ShipRouteObjectiveSource sourceWithErrand(
            String destination, String errandSystem, String station, ReminderContact contact, String... legs) {
        Reminder errand = new Reminder();
        errand.setStarSystem(errandSystem);
        errand.setStationName(station);
        errand.setContact(ReminderContact.nameOrNull(contact));
        errand.setReminder("some sentence written for the voice");
        return new ShipRouteObjectiveSource(() -> destination, () -> legs(legs), () -> errand);
    }

    private static List<NavRouteDto> legs(String... names) {
        List<NavRouteDto> route = new java.util.ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            NavRouteDto dto = new NavRouteDto();
            dto.setLeg(i + 1);
            dto.setName(names[i]);
            route.add(dto);
        }
        return route;
    }
}
