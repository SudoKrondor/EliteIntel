package elite.intel.gameapi.search.spansh.station.shipyard;

import elite.intel.gameapi.search.spansh.station.StationSearchHit;

/**
 * A station whose shipyard sells the wanted hull.
 *
 * @param starSystem  the system it is in, which is what a route is plotted to
 * @param stationName the station itself
 * @param stationType Spansh's own type name, e.g. "Coriolis Starport"; drives how hard it is to dock at
 * @param distanceLy  light years from where the search was measured
 * @param price       credits as Spansh last saw them, or null when Spansh lists the hull without one - it does
 *                    for a hull newly on sale (the Anaconda Mk II, measured 2026-11-01, at every shipyard)
 */
public record ShipyardHit(String starSystem, String stationName, String stationType, double distanceLy,
                          Long price) implements StationSearchHit {

    @Override
    public String getSystemName() {
        return starSystem;
    }

    @Override
    public double getDistance() {
        return distanceLy;
    }
}
