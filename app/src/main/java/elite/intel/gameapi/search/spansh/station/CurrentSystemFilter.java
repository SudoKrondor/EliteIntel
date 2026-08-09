package elite.intel.gameapi.search.spansh.station;

import java.util.List;

/**
 * Drops every hit in the system the commander is currently in, so a station search always points at
 * somewhere they have to travel to.
 *
 * <p>WHY: Spansh's station data is a community snapshot and lags the live galaxy by weeks. A service
 * that was decommissioned — or a material trader that quietly changed to a different type — is still
 * listed, and because it is the nearest hit it wins every time. The commander flies there, finds
 * nothing, asks again, and is sent straight back to the same dead port. Excluding the whole current
 * system breaks that loop and covers the station they are docked at as a matter of course, since
 * that station is by definition in it. It also keeps these commands honest: they end in a galaxy-map
 * route plot, and a route to the system you are already sitting in is not a destination.
 *
 * <p>The tradeoff, taken deliberately: a working service at another port in this same system will
 * not be offered, and the commander is sent to the next system out.
 */
public final class CurrentSystemFilter {

    private CurrentSystemFilter() {
    }

    /**
     * The given hits minus everything in {@code currentSystem}. Never null; may be empty, which
     * callers must treat as "nothing found" rather than falling through to an empty-list access.
     */
    public static <T extends StationSearchHit> List<T> exclude(List<T> hits, String currentSystem) {
        if (hits == null) return List.of();
        return hits.stream().filter(hit -> !isCurrentSystem(hit, currentSystem)).toList();
    }

    /**
     * True when a hit sits in the commander's current system. Two independent tests, because either
     * input can go stale on its own: the reported distance is measured from the coordinates the
     * search was given, so zero means "same system" without trusting session state at all, while the
     * name comparison still catches it when those coordinates were the thing lagging.
     */
    public static boolean isCurrentSystem(StationSearchHit hit, String currentSystem) {
        if (hit == null) return false;
        return hit.getDistance() <= 0.0 || sameName(hit.getSystemName(), currentSystem);
    }

    private static boolean sameName(String a, String b) {
        return a != null && b != null && a.strip().equalsIgnoreCase(b.strip());
    }
}
