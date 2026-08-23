package elite.intel.gameapi.search.spansh.station.refuel;

/**
 * A station the ship can dock at and buy fuel from.
 *
 * @param starSystem  the system it is in, which is what a route is plotted to
 * @param stationName the station itself, which is what the commander has to find once they arrive
 * @param stationType Spansh's own type name, e.g. "Coriolis Starport"; drives how hard it is to dock at
 * @param distanceLy  light years from where the search was measured
 * @param arrivalLs   light seconds from the system's arrival point
 */
public record RefuelStation(String starSystem, String stationName, String stationType,
                            double distanceLy, double arrivalLs) {
}
