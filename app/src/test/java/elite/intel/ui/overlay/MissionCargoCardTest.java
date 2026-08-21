package elite.intel.ui.overlay;

import com.google.gson.JsonParser;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static elite.intel.ui.overlay.HudCards.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The IN HOLD bar on a source-and-return card. The journal reports no progress at all for that
 * mission family - the goods are bought on the open market, so nothing ties the purchase to the
 * mission - and without this the card showed the same "x45" from acceptance to hand-over.
 */
class MissionCargoCardTest {

    private final MissionManager missions = MissionManager.getInstance();
    private GameEvents.CargoEvent hold;

    @BeforeEach
    void clean() {
        missions.clear();
        ShipRouteManager.getInstance().clearRoute();
        hold = null;
    }

    @AfterEach
    void tidy() {
        missions.clear();
    }

    private MissionObjectiveSource source() {
        return new MissionObjectiveSource(missions, () -> null, () -> hold);
    }

    private void accept(long id, String symbol, String localisedCommodity, int count) {
        String json = String.format("""
                { "timestamp":"2026-08-21T07:41:13Z", "event":"MissionAccepted", "Faction":"Cadubii Partners",
                  "Name":"Mission_Collect", "LocalisedName":"Source %d units", "Commodity":"$%s_Name;",
                  "Commodity_Localised":"%s", "Count":%d, "DestinationSystem":"Cadubii",
                  "DestinationStation":"Abe Gateway", "Expiry":"2026-08-22T07:40:51Z", "Wing":false,
                  "Reward":435233, "MissionID":%d }
                """, count, symbol, localisedCommodity, count, id);
        missions.save(new MissionDto(new MissionAcceptedEvent(JsonParser.parseString(json).getAsJsonObject())));
    }

    private void hold(String inventoryJson) {
        hold = GsonFactory.getGson().fromJson(
                "{\"event\":\"Cargo\",\"Vessel\":\"Ship\",\"Count\":99,\"Inventory\":[" + inventoryJson + "]}",
                GameEvents.CargoEvent.class);
    }

    @Test
    @DisplayName("an empty hold reads as none of the cargo aboard, not as no bar")
    void emptyHoldStillDrawsTheBar() {
        accept(1, "Haematite", "Haematite", 18);

        HudRow row = rowOf(source().currentObjective().orElseThrow(), "IN HOLD");

        assertEquals(0, row.current());
        assertEquals(18, row.max());
        assertEquals(HudRow.State.NORMAL, row.state());
    }

    @Test
    @DisplayName("a part load shows how far along it is")
    void partLoadShowsProgress() {
        accept(1, "Haematite", "Haematite", 18);
        hold("{\"Name\":\"haematite\",\"Count\":12,\"Stolen\":0}");

        HudRow row = rowOf(source().currentObjective().orElseThrow(), "IN HOLD");

        assertEquals(12, row.current());
        assertEquals(18, row.max());
        assertEquals(HudRow.State.NORMAL, row.state(), "still short, so not green yet");
    }

    @Test
    @DisplayName("cargo complete turns the bar green")
    void completeCargoIsGood() {
        accept(1, "Haematite", "Haematite", 18);
        hold("{\"Name\":\"haematite\",\"Count\":18,\"Stolen\":0}");

        assertEquals(HudRow.State.GOOD, rowOf(source().currentObjective().orElseThrow(), "IN HOLD").state());
    }

    @Test
    @DisplayName("the cargo row still names the commodity and the amount asked for")
    void cargoRowIsUnchanged() {
        accept(1, "Haematite", "Haematite", 18);

        assertEquals("HAEMATITE x18", valueOf(source().currentObjective().orElseThrow(), "CARGO"));
    }

    @Test
    @DisplayName("a mission with no cargo gets no bar")
    void nonCargoMissionHasNoBar() {
        String donation = """
                { "timestamp":"2026-08-21T07:41:13Z", "event":"MissionAccepted", "Faction":"Cadubii Partners",
                  "Name":"Mission_AltruismCredits", "LocalisedName":"Donate 450,000 Cr", "Reward":0,
                  "Expiry":"2026-08-22T07:40:51Z", "MissionID":7 }
                """;
        missions.save(new MissionDto(new MissionAcceptedEvent(JsonParser.parseString(donation).getAsJsonObject())));

        assertFalse(labels(source().currentObjective().orElseThrow()).contains("IN HOLD"));
    }
}
