package elite.intel.gameapi.gamestate.dtos;

import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Limpets ride in the cargo hold and inflate {@code Count}, which used to make a limpet-only hold
 * look loaded and send the trade-route command to the sell leg with nothing to sell.
 */
class CargoEventTradeableCountTest {

    @Test
    void limpetsDoNotCountAsCargo() {
        GameEvents.CargoEvent cargo = parse("""
                {
                  "timestamp": "2026-07-28T10:00:00Z",
                  "event": "Cargo",
                  "Vessel": "Ship",
                  "Count": 32,
                  "Inventory": [
                    { "Name":"drones", "Name_Localised":"Limpet", "Count":32, "Stolen":0 }
                  ]
                }
                """);

        assertEquals(32, cargo.getCount());
        assertEquals(0, cargo.getTradeableCount());
    }

    @Test
    void tradeGoodsCountWithLimpetsSubtracted() {
        GameEvents.CargoEvent cargo = parse("""
                {
                  "timestamp": "2026-07-28T10:00:00Z",
                  "event": "Cargo",
                  "Vessel": "Ship",
                  "Count": 272,
                  "Inventory": [
                    { "Name":"drones", "Name_Localised":"Limpet", "Count":32, "Stolen":0 },
                    { "Name":"tea", "Count":240, "Stolen":0 }
                  ]
                }
                """);

        assertEquals(240, cargo.getTradeableCount());
    }

    @Test
    void emptyHoldHasNoTradeableCargo() {
        GameEvents.CargoEvent cargo = parse("""
                {
                  "timestamp": "2026-07-28T10:00:00Z",
                  "event": "Cargo",
                  "Vessel": "Ship",
                  "Count": 0,
                  "Inventory": []
                }
                """);

        assertEquals(0, cargo.getTradeableCount());
    }

    @Test
    void missingInventoryFallsBackToReportedCount() {
        GameEvents.CargoEvent cargo = parse("""
                {
                  "timestamp": "2026-07-28T10:00:00Z",
                  "event": "Cargo",
                  "Vessel": "Ship",
                  "Count": 12
                }
                """);

        assertEquals(12, cargo.getTradeableCount());
    }

    private static GameEvents.CargoEvent parse(String json) {
        return GsonFactory.getGson().fromJson(json, GameEvents.CargoEvent.class);
    }
}
