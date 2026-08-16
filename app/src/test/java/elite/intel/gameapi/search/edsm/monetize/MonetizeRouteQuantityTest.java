package elite.intel.gameapi.search.edsm.monetize;

import elite.intel.gameapi.search.edsm.dto.data.Commodity;
import elite.intel.gameapi.search.edsm.dto.data.Station;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the "no point buying four units" rule: a market that cannot fill most of the hold is not
 * worth a docking stop, however good its margin looks per tonne.
 */
class MonetizeRouteQuantityTest {

    private static final int CARGO_CAPACITY = 100; // minimum quantity therefore 80

    private static Station station(String name, String system, Commodity... commodities) {
        Station station = new Station();
        station.name = name;
        station.type = "Coriolis Starport";
        station.setStarSystemName(system);
        station.setCommodities(List.of(commodities));
        return station;
    }

    private static Commodity forSale(String name, int buyPrice, int stock) {
        Commodity commodity = new Commodity();
        commodity.name = name;
        commodity.buyPrice = buyPrice;
        commodity.stock = stock;
        return commodity;
    }

    private static Commodity wanted(String name, int sellPrice, int demand) {
        Commodity commodity = new Commodity();
        commodity.name = name;
        commodity.sellPrice = sellPrice;
        commodity.demand = demand;
        return commodity;
    }

    @Test
    @DisplayName("80% of cargo capacity is the minimum quantity; unknown capacity keeps the old floor")
    void minimumQuantityIsEightyPercentOfCapacity() {
        assertEquals(80, MonetizeRoute.minimumQuantity(100));
        assertEquals(200, MonetizeRoute.minimumQuantity(250));
        assertEquals(6, MonetizeRoute.minimumQuantity(7)); // 5.6 rounds up
        assertEquals(1, MonetizeRoute.minimumQuantity(1));
        assertEquals(2, MonetizeRoute.minimumQuantity(0));
        assertEquals(2, MonetizeRoute.minimumQuantity(-1));
    }

    @Test
    @DisplayName("a huge margin on four units in stock is skipped for a smaller margin that fills the hold")
    void thinSupplyLosesToAFullHold() {
        List<Station> sources = List.of(
                station("Scraps", "Sol", forSale("Painite", 1000, 4)),
                station("Depot", "Sol", forSale("Bauxite", 100, 500))
        );
        List<Station> destinations = List.of(
                station("Buyer", "Alpha Centauri", wanted("Painite", 900000, 500), wanted("Bauxite", 300, 500))
        );

        MonetizeRoute.TradeTransaction trade = MonetizeRoute.findTrade(sources, destinations,
                MonetizeRoute.minimumQuantity(CARGO_CAPACITY));

        assertNotNull(trade);
        assertEquals("Bauxite", trade.getSource().getCommodity());
        assertEquals("Depot", trade.getSource().getStationName());
    }

    @Test
    @DisplayName("a buyer that only wants a handful of tonnes is not a destination")
    void thinDemandIsRejected() {
        List<Station> sources = List.of(station("Depot", "Sol", forSale("Bauxite", 100, 500)));
        List<Station> destinations = List.of(station("Buyer", "Alpha Centauri", wanted("Bauxite", 900000, 4)));

        assertNull(MonetizeRoute.findTrade(sources, destinations, MonetizeRoute.minimumQuantity(CARGO_CAPACITY)));
    }

    @Test
    @DisplayName("supply and demand exactly at the threshold still trade")
    void quantityAtTheThresholdIsAccepted() {
        List<Station> sources = List.of(station("Depot", "Sol", forSale("Bauxite", 100, 80)));
        List<Station> destinations = List.of(station("Buyer", "Alpha Centauri", wanted("Bauxite", 300, 80)));

        MonetizeRoute.TradeTransaction trade = MonetizeRoute.findTrade(sources, destinations,
                MonetizeRoute.minimumQuantity(CARGO_CAPACITY));

        assertNotNull(trade);
        assertEquals("Bauxite", trade.getSource().getCommodity());
        assertEquals(80, trade.getSource().getSupply());
        assertEquals(80, trade.getDestination().getDemand());
    }
}
