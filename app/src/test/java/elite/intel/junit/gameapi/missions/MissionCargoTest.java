package elite.intel.junit.gameapi.missions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MissionCargo;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which mission to go shopping for when a stack of source-and-return missions is on the board, and
 * what is left to buy once the hold is counted.
 */
class MissionCargoTest {

    private static MissionDto mission(long id, String commoditySymbol, int count, String expiry) {
        String json = String.format("""
                { "timestamp":"2026-08-21T07:41:13Z", "event":"MissionAccepted", "Faction":"Cadubii Partners",
                  "Name":"Mission_Collect", "LocalisedName":"Source %d units",
                  "Commodity":"$%s_Name;", "Commodity_Localised":"Something", "Count":%d,
                  "DestinationSystem":"Cadubii", "DestinationStation":"Abe Gateway", %s
                  "Wing":false, "Reward":1, "MissionID":%d }
                """, count, commoditySymbol, count, expiry == null ? "" : "\"Expiry\":\"" + expiry + "\",", id);
        JsonObject event = JsonParser.parseString(json).getAsJsonObject();
        return new MissionDto(new MissionAcceptedEvent(event));
    }

    private static GameEvents.CargoEvent hold(String json) {
        return GsonFactory.getGson().fromJson(json, GameEvents.CargoEvent.class);
    }

    @Test
    @DisplayName("the mission expiring soonest is sourced first")
    void soonestExpiryFirst() {
        List<MissionDto> board = List.of(
                mission(3, "Titanium", 42, "2026-08-23T07:00:00Z"),
                mission(1, "Haematite", 18, "2026-08-21T09:00:00Z"),
                mission(2, "Coffee", 36, "2026-08-22T07:00:00Z"));

        List<MissionCargo.Outstanding> outstanding = MissionCargo.outstanding(board, Map.of());

        assertEquals(List.of("haematite", "coffee", "titanium"),
                outstanding.stream().map(MissionCargo.Outstanding::symbol).toList());
    }

    @Test
    @DisplayName("a mission that never expires is sourced last, not first")
    void missionsWithoutExpirySortLast() {
        List<MissionDto> board = List.of(
                mission(1, "Coffee", 36, null),
                mission(2, "Haematite", 18, "2026-08-25T07:00:00Z"));

        assertEquals("haematite", MissionCargo.nextToSource(board, Map.of()).orElseThrow().symbol());
    }

    @Test
    @DisplayName("cargo already in the hold is not searched for again")
    void heldCargoIsSkipped() {
        List<MissionDto> board = List.of(
                mission(1, "Haematite", 18, "2026-08-21T09:00:00Z"),
                mission(2, "Coffee", 36, "2026-08-22T07:00:00Z"));

        Optional<MissionCargo.Outstanding> next = MissionCargo.nextToSource(board, Map.of("haematite", 18));

        assertEquals("coffee", next.orElseThrow().symbol());
        assertEquals(36, next.orElseThrow().shortfall());
    }

    @Test
    @DisplayName("a stacked commodity is allocated across missions, not counted twice")
    void stackedMissionsShareTheHold() {
        // 18 + 72 units of Haematite are owed between them; 72 in the hold covers the first outright and
        // leaves the second still owing 18. Comparing each mission against the raw hold would call both done.
        List<MissionDto> board = List.of(
                mission(1, "Haematite", 18, "2026-08-21T09:00:00Z"),
                mission(2, "Haematite", 72, "2026-08-22T07:00:00Z"));

        List<MissionCargo.Outstanding> outstanding = MissionCargo.outstanding(board, Map.of("haematite", 72));

        assertTrue(outstanding.get(0).isSatisfied());
        assertEquals(18, outstanding.get(0).held());
        assertFalse(outstanding.get(1).isSatisfied());
        assertEquals(54, outstanding.get(1).held());
        assertEquals(18, outstanding.get(1).shortfall());
        assertEquals(18, MissionCargo.nextToSource(board, Map.of("haematite", 72)).orElseThrow().shortfall());
    }

    @Test
    @DisplayName("every requirement covered is a different answer to nothing to do")
    void satisfiedBoardIsNotAnEmptyBoard() {
        List<MissionDto> board = List.of(mission(1, "Haematite", 18, "2026-08-21T09:00:00Z"));

        assertEquals(1, MissionCargo.outstanding(board, Map.of("haematite", 20)).size());
        assertTrue(MissionCargo.nextToSource(board, Map.of("haematite", 20)).isEmpty());
    }

    @Test
    @DisplayName("missions that want no cargo are not a shopping list")
    void donationsAreIgnored() {
        String donation = """
                { "timestamp":"2026-08-21T07:41:13Z", "event":"MissionAccepted", "Faction":"Cadubii Partners",
                  "Name":"Mission_AltruismCredits", "LocalisedName":"Donate 450,000 Cr to the cause",
                  "Expiry":"2026-08-21T08:00:00Z", "Wing":false, "Reward":0, "MissionID":9 }
                """;
        MissionDto board = new MissionDto(new MissionAcceptedEvent(JsonParser.parseString(donation).getAsJsonObject()));

        assertTrue(MissionCargo.outstanding(List.of(board), Map.of()).isEmpty());
    }

    @Test
    @DisplayName("the hold is read by symbol, however the journal cased it")
    void holdIsKeyedBySymbol() {
        GameEvents.CargoEvent cargo = hold("""
                { "event":"Cargo", "Vessel":"Ship", "Count":90, "Inventory":[
                  { "Name":"haematite", "Count":72, "Stolen":0 },
                  { "Name":"Drones", "Count":18, "Stolen":0 } ] }
                """);

        Map<String, Integer> held = MissionCargo.heldBySymbol(cargo);

        assertEquals(72, held.get("haematite"));
        assertEquals(18, held.get("drones"));
    }

    @Test
    @DisplayName("an empty hold is not a crash")
    void emptyHold() {
        assertTrue(MissionCargo.heldBySymbol(null).isEmpty());
        assertTrue(MissionCargo.heldBySymbol(hold("{ \"event\":\"Cargo\", \"Count\":0 }")).isEmpty());
    }
}
