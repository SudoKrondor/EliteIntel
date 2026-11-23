package elite.intel.ui.overlay;

import com.google.gson.JsonParser;
import elite.intel.db.managers.MissionManager;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.journal.events.CargoDepotEvent;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.journal.subscribers.CargoDepotSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static elite.intel.ui.overlay.HudCards.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Mining missions stack at the board like massacre contracts, but each one is handed its own tonnes
 * at the depot. The card showed the featured mission's "x200" for a stack that needed 1,000, so the
 * mining layout totals each mineral across the stack, less what has been delivered.
 */
class MiningMissionCardTest {

    private final MissionManager missions = MissionManager.getInstance();
    private final Map<String, Integer> hold = new HashMap<>();

    @BeforeEach
    void clean() {
        missions.clear();
        ShipRouteManager.getInstance().clearRoute();
        hold.clear();
    }

    @AfterEach
    void tidy() {
        missions.clear();
    }

    private MissionObjectiveSource source() {
        return new MissionObjectiveSource(missions, () -> null, () -> hold);
    }

    private void acceptMining(long id, String symbol, int count, String expiry) {
        String json = String.format("""
                { "timestamp":"2026-11-23T01:10:00Z", "event":"MissionAccepted", "Faction":"Carbon Miners Guild",
                  "Name":"Mission_Mining_Expansion_name", "LocalisedName":"Mine %d units of %s",
                  "Commodity":"$%s_Name;", "Commodity_Localised":"%s", "Count":%d,
                  "Expiry":"%s", "Wing":false, "Reward":1000000, "MissionID":%d }
                """, count, symbol, symbol, symbol, count, expiry, id);
        missions.save(new MissionDto(new MissionAcceptedEvent(JsonParser.parseString(json).getAsJsonObject())));
    }

    private void acceptCourier(long id, String expiry) {
        String json = String.format("""
                { "timestamp":"2026-11-23T01:10:00Z", "event":"MissionAccepted", "Faction":"Carbon Miners Guild",
                  "Name":"Mission_Courier", "LocalisedName":"Courier job", "DestinationSystem":"Sol",
                  "Expiry":"%s", "Wing":false, "Reward":50000, "MissionID":%d }
                """, expiry, id);
        missions.save(new MissionDto(new MissionAcceptedEvent(JsonParser.parseString(json).getAsJsonObject())));
    }

    private void delivered(long id, String symbol, int soFar, int total) {
        String json = String.format("""
                { "timestamp":"2026-11-23T02:00:00Z", "event":"CargoDepot", "MissionID":%d, "UpdateType":"Deliver",
                  "CargoType":"%s", "Count":%d, "StartMarketID":0, "EndMarketID":3223330304,
                  "ItemsCollected":0, "ItemsDelivered":%d, "TotalItemsToDeliver":%d, "Progress":0.0 }
                """, id, symbol, soFar, soFar, total);
        new CargoDepotSubscriber().onCargoDepot(new CargoDepotEvent(JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    @DisplayName("five missions for one mineral total that mineral, not one mission's share")
    void totalsTheMineralAcrossTheStack() {
        for (int i = 1; i <= 5; i++) acceptMining(i, "Bertrandite", 200, "2026-12-01T00:00:0" + i + "Z");

        HudObjective card = source().currentObjective().orElseThrow();

        assertEquals("MINING MISSIONS", card.title());
        assertEquals("0 / 1,000 T", valueOf(card, "BERTRANDITE"));
        assertEquals("5", valueOf(card, "MISSIONS"));
        assertEquals("5,000,000 cr", valueOf(card, "STACK REWARD"));
    }

    @Test
    @DisplayName("each mineral gets its own total, the featured one first")
    void splitsByMineral() {
        acceptMining(1, "Gold", 100, "2026-12-01T00:00:00Z");
        acceptMining(2, "Bertrandite", 200, "2026-12-02T00:00:00Z");
        acceptMining(3, "Gold", 50, "2026-12-03T00:00:00Z");

        HudObjective card = source().currentObjective().orElseThrow();

        assertEquals(List.of("GOLD", "BERTRANDITE", "MISSIONS", "STACK REWARD", "EXPIRES"), labels(card));
        assertEquals("0 / 150 T", valueOf(card, "GOLD"));
        assertEquals("0 / 200 T", valueOf(card, "BERTRANDITE"));
    }

    @Test
    @DisplayName("tonnes already handed in at the depot are no longer owed")
    void deliveriesComeOffTheTotal() {
        acceptMining(1, "Bertrandite", 200, "2026-12-01T00:00:00Z");
        acceptMining(2, "Bertrandite", 200, "2026-12-02T00:00:00Z");

        delivered(1, "Bertrandite", 40, 200);
        delivered(1, "Bertrandite", 120, 200);

        assertEquals(120, missions.getMission(1).getItemsDelivered());
        assertEquals("0 / 280 T", valueOf(source().currentObjective().orElseThrow(), "BERTRANDITE"));
    }

    @Test
    @DisplayName("an older depot update never winds the delivered count back")
    void deliveriesOnlyRise() {
        acceptMining(1, "Bertrandite", 200, "2026-12-01T00:00:00Z");

        delivered(1, "Bertrandite", 120, 200);
        delivered(1, "Bertrandite", 40, 200);

        assertEquals(120, missions.getMission(1).getItemsDelivered());
    }

    @Test
    @DisplayName("enough aboard for the whole mineral turns its row green")
    void heldCoversTheMineral() {
        acceptMining(1, "Bertrandite", 200, "2026-12-01T00:00:00Z");
        acceptMining(2, "Bertrandite", 200, "2026-12-02T00:00:00Z");
        hold.put("bertrandite", 250);

        HudRow partial = rowOf(source().currentObjective().orElseThrow(), "BERTRANDITE");
        assertEquals("250 / 400 T", partial.value());
        assertEquals(HudRow.State.NORMAL, partial.state());

        hold.put("bertrandite", 450);
        HudRow full = rowOf(source().currentObjective().orElseThrow(), "BERTRANDITE");
        assertEquals("400 / 400 T", full.value(), "more aboard than owed reads as covered, not as a bigger number");
        assertEquals(HudRow.State.GOOD, full.state());
    }

    @Test
    @DisplayName("a courier sitting alongside is neither counted nor given a row")
    void otherFamiliesStayOut() {
        acceptMining(1, "Bertrandite", 200, "2026-12-01T00:00:00Z");
        acceptMining(2, "Bertrandite", 200, "2026-12-02T00:00:00Z");
        acceptCourier(3, "2026-12-05T00:00:00Z");

        HudObjective card = source().currentObjective().orElseThrow();

        assertEquals("2", valueOf(card, "MISSIONS"));
        assertEquals(List.of("BERTRANDITE", "MISSIONS", "STACK REWARD", "EXPIRES"), labels(card));
    }

    @Test
    @DisplayName("more minerals than rows end in a count of the rest, and the expiry survives")
    void tooManyMineralsOverflow() {
        String[] minerals = {"Gold", "Silver", "Bertrandite", "Indite", "Gallite", "Coltan", "Uraninite"};
        for (int i = 0; i < minerals.length; i++) {
            acceptMining(i + 1, minerals[i], 100, "2026-12-01T00:00:0" + i + "Z");
        }

        List<String> labels = labels(source().currentObjective().orElseThrow());

        assertTrue(labels.size() <= 8, labels.toString());
        assertEquals("+3", valueOf(source().currentObjective().orElseThrow(), "MORE GOODS"));
        assertEquals("EXPIRES", labels.getLast());
    }

    @Test
    @DisplayName("a lone mining mission keeps its own reward label and no mission count")
    void singleMission() {
        acceptMining(1, "Bertrandite", 200, "2026-12-01T00:00:00Z");

        HudObjective card = source().currentObjective().orElseThrow();

        assertEquals(List.of("BERTRANDITE", "REWARD", "EXPIRES"), labels(card));
        assertFalse(labels(card).contains("MISSIONS"));
    }
}
