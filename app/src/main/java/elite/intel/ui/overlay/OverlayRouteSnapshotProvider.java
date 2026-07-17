package elite.intel.ui.overlay;

import elite.intel.db.managers.ShipRouteManager;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;
import elite.intel.search.spansh.station.marketstation.TradeStopDto;
import elite.intel.search.spansh.traderoute.TradeCommodity;
import elite.intel.session.PlayerSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reads persisted route state off the EDT and turns it into small immutable overlay snapshots. Trade route state
 * intentionally wins over ordinary navigation because plotting a route to a trade stop commonly activates both.
 */
final class OverlayRouteSnapshotProvider {

    private final ShipRouteManager shipRouteManager;
    private final TradeRouteManager tradeRouteManager;
    private final PlayerSession playerSession;

    /** Creates the production reader backed by the existing route managers and player session. */
    OverlayRouteSnapshotProvider() {
        this(ShipRouteManager.getInstance(), TradeRouteManager.getInstance(), PlayerSession.getInstance());
    }

    /** Test seam for providing the route managers and player session explicitly. */
    OverlayRouteSnapshotProvider(
            ShipRouteManager shipRouteManager,
            TradeRouteManager tradeRouteManager,
            PlayerSession playerSession
    ) {
        this.shipRouteManager = Objects.requireNonNull(shipRouteManager, "shipRouteManager");
        this.tradeRouteManager = Objects.requireNonNull(tradeRouteManager, "tradeRouteManager");
        this.playerSession = Objects.requireNonNull(playerSession, "playerSession");
    }

    /** Loads one coherent display snapshot. This method may perform database reads and must not run on the EDT. */
    OverlayRouteSnapshot load() {
        List<NavRouteDto> navigation = shipRouteManager.getOrderedRoute();
        List<TradeStopDto> tradeStops = readTradeStops();
        return from(navigation, tradeStops, cargoCount());
    }

    /**
     * Pure route-selection rule shared by production loading and unit tests. An active trade route overrides the
     * main card while its normal navigation data remains attached as a subordinate readout.
     */
    static OverlayRouteSnapshot from(
            List<NavRouteDto> navigation,
            List<TradeStopDto> tradeStops,
            int cargoCount
    ) {
        NavigationRouteSnapshot navigationSnapshot = navigationSnapshot(navigation);
        List<TradeStopDto> stops = tradeStops == null
                ? List.of()
                : tradeStops.stream().filter(Objects::nonNull).toList();
        if (stops.isEmpty()) {
            return navigationSnapshot == null ? new NoRouteSnapshot() : navigationSnapshot;
        }

        TradeStopDto nextStop = stops.getFirst();
        TradeRouteAction action = cargoCount > 0 ? TradeRouteAction.SELL : TradeRouteAction.BUY;
        boolean sell = action == TradeRouteAction.SELL;
        return new TradeRouteSnapshot(
                action,
                sell ? 2 : 1,
                stops.size() * 2,
                sell ? nextStop.getDestinationSystem() : nextStop.getSourceSystem(),
                sell ? nextStop.getDestinationStation() : nextStop.getSourceStation(),
                commoditySummary(nextStop.getCommodities()),
                projectedProfit(nextStop.getCommodities()),
                navigationSnapshot);
    }

    private List<TradeStopDto> readTradeStops() {
        List<TradeRouteManager.TradeRouteLegTuple<Integer, TradeStopDto>> tuples =
                new ArrayList<>(tradeRouteManager.getAllStops());
        tuples.sort(Comparator.comparing(
                tuple -> tuple.getLegNumber() == null ? Integer.MAX_VALUE : tuple.getLegNumber()));
        return tuples.stream()
                .map(TradeRouteManager.TradeRouteLegTuple::getTradeStopDto)
                .filter(Objects::nonNull)
                .toList();
    }

    private int cargoCount() {
        GameEvents.CargoEvent cargo = playerSession.getShipCargo();
        return cargo == null ? 0 : cargo.getCount();
    }

    private static NavigationRouteSnapshot navigationSnapshot(List<NavRouteDto> route) {
        if (route == null || route.isEmpty()) {
            return null;
        }
        List<NavRouteDto> ordered = route.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(NavRouteDto::getLeg))
                .toList();
        if (ordered.isEmpty()) {
            return null;
        }
        NavRouteDto next = ordered.getFirst();
        NavRouteDto destination = ordered.getLast();
        return new NavigationRouteSnapshot(
                next.getName(),
                destination.getName(),
                ordered.size(),
                next.getStarClass(),
                next.isScoopable());
    }

    private static String commoditySummary(List<TradeCommodity> commodities) {
        if (commodities == null || commodities.isEmpty()) {
            return "";
        }
        return commodities.stream()
                .filter(Objects::nonNull)
                .map(TradeCommodity::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static long projectedProfit(List<TradeCommodity> commodities) {
        if (commodities == null) {
            return 0L;
        }
        return commodities.stream()
                .filter(Objects::nonNull)
                .mapToLong(TradeCommodity::getTotalProfit)
                .sum();
    }
}
