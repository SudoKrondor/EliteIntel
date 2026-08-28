package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How much a sell search asks for is what the ship is actually carrying: a market wanting 40 tonnes is no
 * answer for the 300 aboard.
 *
 * <p>The join is the part that can fail silently. The hold speaks journal symbols ({@code tritium}) and the
 * search speaks the English name Spansh matches ({@code Tritium}), so a broken lookup returns nothing found
 * rather than an error - and the search then quietly asks for a whole hold's worth of demand on behalf of a
 * commander carrying twelve tonnes.
 */
class SellCommodityHoldTest {

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @Test
    void theTonnageAskedForIsWhatIsInTheHold() {
        loadHold("""
                {"event":"Cargo","Vessel":"Ship","Count":312,
                 "Inventory":[{"Name":"tritium","Count":300},{"Name":"drones","Count":12}]}""");

        assertEquals(300, CommodityTradeSearch.tonnesInHold("Tritium"));
    }

    @Test
    void aGoodWeAreNotCarryingIsZeroSoTheSearchFallsBackToAHoldsWorth() {
        loadHold("""
                {"event":"Cargo","Vessel":"Ship","Count":300,
                 "Inventory":[{"Name":"tritium","Count":300}]}""");

        assertEquals(0, CommodityTradeSearch.tonnesInHold("Gold"),
                "asking where to sell a good we are not carrying is a planning question, not an errand");
    }

    @Test
    void anEmptyHoldIsZeroRatherThanAFailure() {
        loadHold("""
                {"event":"Cargo","Vessel":"Ship","Count":0,"Inventory":[]}""");

        assertEquals(0, CommodityTradeSearch.tonnesInHold("Tritium"));
    }

    @Test
    void aCommodityTheTableDoesNotKnowIsZero() {
        loadHold("""
                {"event":"Cargo","Vessel":"Ship","Count":300,
                 "Inventory":[{"Name":"tritium","Count":300}]}""");

        assertEquals(0, CommodityTradeSearch.tonnesInHold("Nonexistent Widget"));
    }

    private static void loadHold(String cargoJson) {
        PlayerSession.getInstance().setShipCargo(
                GsonFactory.getGson().fromJson(cargoJson, GameEvents.CargoEvent.class));
    }
}
