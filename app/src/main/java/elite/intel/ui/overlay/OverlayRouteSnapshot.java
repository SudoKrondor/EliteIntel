package elite.intel.ui.overlay;

import java.util.Objects;

/** Immutable route state rendered by {@link RouteOverlayModule}; it never exposes database entities to Swing. */
sealed interface OverlayRouteSnapshot permits NoRouteSnapshot, NavigationRouteSnapshot, TradeRouteSnapshot {
}

/** Represents the absence of both a normal navigation route and a trade route. */
record NoRouteSnapshot() implements OverlayRouteSnapshot {
}

/** The next and final points of the current ordinary navigation route. */
record NavigationRouteSnapshot(
        String nextSystem,
        String destinationSystem,
        int remainingJumps,
        String starClass,
        boolean scoopable
) implements OverlayRouteSnapshot {

    NavigationRouteSnapshot {
        nextSystem = clean(nextSystem);
        destinationSystem = clean(destinationSystem);
        starClass = clean(starClass);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

/** The immediate action of the active trade route, with ordinary route data retained as a subordinate readout. */
record TradeRouteSnapshot(
        TradeRouteAction action,
        int pointNumber,
        int pointCount,
        String system,
        String station,
        String cargo,
        long projectedProfit,
        NavigationRouteSnapshot navigation
) implements OverlayRouteSnapshot {

    TradeRouteSnapshot {
        action = Objects.requireNonNull(action, "action");
        system = clean(system);
        station = clean(station);
        cargo = clean(cargo);
        pointNumber = Math.max(1, pointNumber);
        pointCount = Math.max(pointNumber, pointCount);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

/** The next trade-route operation inferred from the same cargo rule used by navigation commands. */
enum TradeRouteAction {
    BUY,
    SELL
}
