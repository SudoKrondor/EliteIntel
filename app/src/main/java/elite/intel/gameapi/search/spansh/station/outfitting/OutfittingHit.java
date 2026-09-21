package elite.intel.gameapi.search.spansh.station.outfitting;

import elite.intel.gameapi.search.spansh.station.StationSearchHit;

import java.util.List;

/**
 * A station whose outfitting stocks the wanted module, and what it stocks.
 *
 * @param starSystem  the system it is in, which is what a route is plotted to
 * @param stationName the station itself, which is what the commander has to find once they arrive
 * @param stationType Spansh's own type name, e.g. "Coriolis Starport"; drives how hard it is to dock at
 * @param distanceLy  light years from where the search was measured
 * @param modules     every listing that answered the search, cheapest first; never empty
 */
public record OutfittingHit(String starSystem, String stationName, String stationType, double distanceLy,
                            List<StockedModule> modules) implements StationSearchHit {

    public OutfittingHit {
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("An outfitting hit must stock at least one matching module");
        }
        modules = List.copyOf(modules);
    }

    /**
     * The cheapest listing's price, which is what the station is ranked on.
     */
    public long cheapest() {
        return modules.getFirst().price();
    }

    @Override
    public String getSystemName() {
        return starSystem;
    }

    @Override
    public double getDistance() {
        return distanceLy;
    }
}
