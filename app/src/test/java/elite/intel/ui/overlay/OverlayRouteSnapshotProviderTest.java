package elite.intel.ui.overlay;

import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.search.spansh.station.marketstation.TradeStopDto;
import elite.intel.search.spansh.traderoute.TradeCommodity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OverlayRouteSnapshotProviderTest {

    @Test
    void activeTradeRouteOverridesNavigationAndKeepsItsJumpReadout() {
        OverlayRouteSnapshot snapshot = OverlayRouteSnapshotProvider.from(
                List.of(route(1, "Lave", "G", true), route(2, "Leesti", "K", true)),
                List.of(tradeStop("Lave", "Lave Station", "Leesti", "George Lucas", "Silver", 84_000L)),
                0);

        TradeRouteSnapshot trade = assertInstanceOf(TradeRouteSnapshot.class, snapshot);
        assertEquals(TradeRouteAction.BUY, trade.action());
        assertEquals(1, trade.pointNumber());
        assertEquals(2, trade.pointCount());
        assertEquals("Lave", trade.system());
        assertEquals("Lave Station", trade.station());
        assertEquals("Silver", trade.cargo());
        assertEquals(84_000L, trade.projectedProfit());
        assertEquals("Lave", trade.navigation().nextSystem());
        assertEquals(2, trade.navigation().remainingJumps());
    }

    @Test
    void loadedCargoSwitchesTheTradeCardToItsSellPoint() {
        OverlayRouteSnapshot snapshot = OverlayRouteSnapshotProvider.from(
                List.of(),
                List.of(tradeStop("Lave", "Lave Station", "Leesti", "George Lucas", "Silver", 84_000L)),
                12);

        TradeRouteSnapshot trade = assertInstanceOf(TradeRouteSnapshot.class, snapshot);
        assertEquals(TradeRouteAction.SELL, trade.action());
        assertEquals(2, trade.pointNumber());
        assertEquals("Leesti", trade.system());
        assertEquals("George Lucas", trade.station());
        assertEquals(null, trade.navigation());
    }

    @Test
    void ordinaryNavigationIsShownWhenNoTradeRouteExists() {
        OverlayRouteSnapshot snapshot = OverlayRouteSnapshotProvider.from(
                List.of(route(1, "Lave", "G", true), route(2, "Leesti", "K", false)),
                List.of(),
                0);

        NavigationRouteSnapshot navigation = assertInstanceOf(NavigationRouteSnapshot.class, snapshot);
        assertEquals("Lave", navigation.nextSystem());
        assertEquals("Leesti", navigation.destinationSystem());
        assertEquals(2, navigation.remainingJumps());
        assertEquals("G", navigation.starClass());
        assertEquals(true, navigation.scoopable());
    }

    @Test
    void noRouteProducesAnExplicitEmptySnapshot() {
        OverlayRouteSnapshot snapshot = OverlayRouteSnapshotProvider.from(List.of(), List.of(), 0);

        assertInstanceOf(NoRouteSnapshot.class, snapshot);
    }

    private static NavRouteDto route(int leg, String system, String starClass, boolean scoopable) {
        NavRouteDto route = new NavRouteDto();
        route.setLeg(leg);
        route.setName(system);
        route.setStarClass(starClass);
        route.setScoopable(scoopable);
        return route;
    }

    private static TradeStopDto tradeStop(
            String sourceSystem,
            String sourceStation,
            String destinationSystem,
            String destinationStation,
            String commodityName,
            long profit
    ) {
        TradeCommodity commodity = new TradeCommodity();
        commodity.name = commodityName;
        commodity.totalProfit = profit;
        return new TradeStopDto(
                1,
                List.of(commodity),
                sourceSystem,
                sourceStation,
                destinationSystem,
                destinationStation,
                1L,
                2L);
    }
}
