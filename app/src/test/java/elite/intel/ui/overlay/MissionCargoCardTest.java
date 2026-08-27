package elite.intel.ui.overlay;

import com.google.gson.JsonParser;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MissionCargo;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static elite.intel.ui.overlay.HudCards.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The IN HOLD bar. Two mission families need it and the journal reports progress for neither: a
 * source-and-return mission is filled by buying on the open market, so nothing ties the purchase to
 * the mission, and an on-foot mission's item never enters the cargo hold at all. Without this the
 * card showed the same "x45" from acceptance to hand-over.
 */
class MissionCargoCardTest {

    private final MissionManager missions = MissionManager.getInstance();
    private GameEvents.CargoEvent hold;
    private final List<GameEvents.MicroResource> suitInventory = new ArrayList<>();

    @BeforeEach
    void clean() {
        missions.clear();
        ShipRouteManager.getInstance().clearRoute();
        hold = null;
        suitInventory.clear();
    }

    @AfterEach
    void tidy() {
        missions.clear();
    }

    private MissionObjectiveSource source() {
        return new MissionObjectiveSource(missions, () -> null,
                () -> MissionCargo.heldBySymbol(hold, suitInventory));
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

    /**
     * An Odyssey mission: the item asked for is a micro-resource, never ship cargo.
     */
    private void acceptOnFoot(long id, String symbol, String localisedCommodity, int count) {
        String json = String.format("""
                { "timestamp":"2026-08-27T15:45:58Z", "event":"MissionAccepted", "Faction":"Union of Hidar Free",
                  "Name":"Mission_OnFoot_Salvage_MB", "LocalisedName":"Grab the %s from a crash site",
                  "Commodity":"$%s_Name;", "Commodity_Localised":"%s", "Count":%d,
                  "DestinationSystem":"Hidar", "DestinationStation":"Gerrold Terminal",
                  "Expiry":"2026-08-27T21:33:27Z", "Wing":false, "Reward":87018, "MissionID":%d }
                """, localisedCommodity, symbol, localisedCommodity, count, id);
        missions.save(new MissionDto(new MissionAcceptedEvent(JsonParser.parseString(json).getAsJsonObject())));
    }

    /**
     * What {@code PlayerSession.getSuitInventory()} would hand over: the live micro-resources.
     */
    private void carrying(String itemsJson) {
        suitInventory.addAll(GsonFactory.getGson().fromJson(
                "{\"event\":\"ShipLocker\",\"Items\":[" + itemsJson + "]}",
                GameEvents.ShipLockerEvent.class).getItems());
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
    @DisplayName("an on-foot mission's item counts even though the cargo hold has never held it")
    void suitInventoryCounts() {
        // The reported bug: the sample was grabbed, the game had already redirected the mission to its
        // hand-in, and the card still read 0/1 because it only ever looked at Cargo.json. Which of the
        // two suit inventory files is live is PlayerSession's decision, pinned in
        // SuitInventoryTest; here it is already resolved.
        acceptOnFoot(1064450663, "ChemicalSample", "Chemical Sample", 1);
        hold("{\"Name\":\"advancedcatalysers\",\"Count\":1,\"Stolen\":0}");
        carrying("{\"Name\":\"chemicalsample\",\"Name_Localised\":\"Chemical Sample\",\"OwnerID\":0,\"MissionID\":1064450663,\"Count\":1}");

        HudRow row = rowOf(source().currentObjective().orElseThrow(), "IN HOLD");

        assertEquals(1, row.current());
        assertEquals(1, row.max());
        assertEquals(HudRow.State.GOOD, row.state());
    }

    @Test
    @DisplayName("a part-collected on-foot stack still shows how far along it is")
    void partCollectedSuitInventoryShowsProgress() {
        acceptOnFoot(1064450663, "ChemicalSample", "Chemical Sample", 3);
        carrying("{\"Name\":\"chemicalsample\",\"Name_Localised\":\"Chemical Sample\",\"OwnerID\":0,\"MissionID\":1064450663,\"Count\":1}");

        HudRow row = rowOf(source().currentObjective().orElseThrow(), "IN HOLD");

        assertEquals(1, row.current());
        assertEquals(3, row.max());
        assertEquals(HudRow.State.NORMAL, row.state());
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
