package elite.intel.gameapi.search.spansh.station.shipyard;

import elite.intel.gameapi.search.PermitLockedSystems;
import elite.intel.gameapi.search.spansh.station.CurrentSystemFilter;
import elite.intel.gameapi.search.spansh.station.DockingEffort;
import elite.intel.gameapi.search.spansh.station.SearchRadii;
import elite.intel.gameapi.search.spansh.station.StationSearchClient;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where to buy a ship: the stations whose shipyard sells the hull, as Spansh last saw them.
 *
 * <p>The hull sibling of {@code OutfittingSearch}, on the same endpoint with a ship filter instead of a module
 * one. The shipyard the commander is docked at needs no search - he is looking at it - so this is only ever
 * about somewhere else.
 *
 * <p>WHY price is still a ranking: hull prices are fixed by the game, so a station is only ever cheaper because
 * a Power's discount applies there, which on a hundred-million-credit hull is worth a jump. Between stations at
 * the same price, nearest wins.
 */
public final class ShipyardSearch {

    /**
     * Only a shipyard Spansh has seen this recently is trusted, in its own relative-time vocabulary. Filtered on
     * the shipyard's own upload time, not the station's, for the reason the market search gives.
     */
    static final String FRESHNESS_WINDOW = "now-7d";

    /**
     * How far from the arrival point a station may sit and still be worth the trip, in light seconds.
     */
    static final int MAX_ARRIVAL_LS = 25_000;

    /**
     * Comfortably above any station's pad count; the range is really "one or more".
     */
    private static final int PADS_MANY = 100;

    /**
     * Stations to weigh per request. Each hit carries the station's whole market, outfitting and shipyard - a
     * page of ten measured about two megabytes - so the cheapest is picked out of the nearest ten.
     */
    private static final int CANDIDATES = 10;

    private static final Logger log = LogManager.getLogger(ShipyardSearch.class);

    private ShipyardSearch() {
    }

    /**
     * The stations within reach that sell the hull, best first, outside the commander's current system.
     *
     * <p>The radius widens as {@link SearchRadii#widening} says rather than report that a hull which exists is
     * sold nowhere; the caller compares the winner's distance against what was asked for and says so.
     *
     * @param ship             the hull
     * @param currentSystem    the system the ship is in, whose stations are never the answer - see
     *                         {@link CurrentSystemFilter}
     * @param maxDistanceLy    the radius to try first
     * @param requiresLargePad when true only stations with a large pad are candidates
     * @param returnClosest    true for the nearest shipyard, false for the cheapest
     * @return the stations found, ranked; empty when even the widest sweep found none
     */
    public static List<ShipyardHit> find(ShipyardShip ship, double x, double y, double z, String currentSystem,
                                         int maxDistanceLy, boolean requiresLargePad, boolean returnClosest) {
        for (int radius : SearchRadii.widening(maxDistanceLy)) {
            TradeStationSearchCriteria criteria = searchCriteria(ship, x, y, z, radius, requiresLargePad);
            log.debug("Shipyard search criteria: {}", criteria.toJson());
            TradeStationSearchResultDto response = StationSearchClient.getInstance().searchStations(criteria);
            // A failed POST, a search that times out and an empty body all arrive here as a null.
            if (response == null || response.getResults() == null) continue;
            List<ShipyardHit> found = rank(PermitLockedSystems.reachable(
                    CurrentSystemFilter.exclude(selling(response.getResults(), ship), currentSystem)), returnClosest);
            if (!found.isEmpty()) return found;
            log.debug("No shipyard selling {} within {} ly; widening", ship.name(), radius);
        }
        return List.of();
    }

    /**
     * The request body. Package-private so the wire shape can be asserted without a live search: Spansh ignores
     * a filter key it does not know and matches nothing against a name it does not know, so either mistake
     * reads as "nobody sells that ship".
     */
    static TradeStationSearchCriteria searchCriteria(
            ShipyardShip ship, double x, double y, double z, int radius, boolean requiresLargePad) {

        TradeStationSearchCriteria.StationType stationType = new TradeStationSearchCriteria.StationType();
        // Carriers left out for the reason the commodity search gives: one may have jumped by the time we arrive.
        stationType.setTypes(TradeStationSearchCriteria.StationType.EVERY_STATIC_TRADE_TYPE);

        // A min/max pair of STRINGS on this endpoint; see OutfittingSearch.
        TradeStationSearchCriteria.Distance distance = new TradeStationSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(radius);

        TradeStationSearchCriteria.UpdatedAt seen = new TradeStationSearchCriteria.UpdatedAt();
        seen.setComparison("<=>");
        seen.setValue(List.of(FRESHNESS_WINDOW, "now"));

        TradeStationSearchCriteria.Filters filters = new TradeStationSearchCriteria.Filters();
        filters.setStationType(stationType);
        filters.setDistanceToStarSystem(distance);
        filters.setDistanceToArrival(new TradeStationSearchCriteria.RangeFilter(0, MAX_ARRIVAL_LS));
        filters.setShips(new TradeStationSearchCriteria.Ships(List.of(ship.name())));
        filters.setShipyardUpdatedAt(seen);
        if (requiresLargePad) {
            filters.setLargePads(new TradeStationSearchCriteria.RangeFilter(1, PADS_MANY));
        }

        TradeStationSearchCriteria criteria = new TradeStationSearchCriteria();
        criteria.setFilters(filters);
        criteria.setReferenceCoords(new TradeStationSearchCriteria.ReferenceCoords(x, y, z));
        // Without a server-side sort page 0 is ten arbitrary matches out of the whole radius.
        criteria.setSort(List.of(new TradeStationSearchCriteria.DistanceSort()));
        criteria.setSize(CANDIDATES);
        criteria.setPage(0);
        return criteria;
    }

    /**
     * The page as hits: every station whose shipyard actually lists the hull, re-checked against what Spansh
     * sent rather than trusted, with the hull's price there.
     */
    static List<ShipyardHit> selling(List<TradeStationSearchResultDto.StationResult> stations, ShipyardShip ship) {
        List<ShipyardHit> hits = new ArrayList<>();
        for (TradeStationSearchResultDto.StationResult station : stations) {
            if (station.getName() == null || station.getSystemName() == null || station.getShips() == null) continue;
            station.getShips().stream()
                    .filter(listed -> isTheShip(listed, ship))
                    .findFirst()
                    .ifPresent(listed -> hits.add(new ShipyardHit(station.getSystemName(), station.getName(),
                            station.getType(), station.getDistance() == null ? 0 : station.getDistance(),
                            listed.getPrice())));
        }
        return hits;
    }

    private static boolean isTheShip(TradeStationSearchResultDto.StationResult.Ship listed, ShipyardShip ship) {
        return ship.symbol().equalsIgnoreCase(listed.getSymbol()) || ship.name().equalsIgnoreCase(listed.getName());
    }

    /**
     * Puts the answer the commander asked for at the head: the nearest shipyard, or the cheapest and then the
     * nearest - a hull listed without a price ranks after every priced one, since nothing is known to be saved
     * there. Easiest to dock at comes before either, for the reason {@link DockingEffort} gives.
     */
    static List<ShipyardHit> rank(List<ShipyardHit> hits, boolean returnClosest) {
        List<ShipyardHit> sorted = new ArrayList<>(hits);
        Comparator<ShipyardHit> byDistance = Comparator.comparingDouble(ShipyardHit::distanceLy);
        Comparator<ShipyardHit> byPrice = Comparator.comparingLong(
                hit -> hit.price() == null ? Long.MAX_VALUE : hit.price());
        sorted.sort(Comparator.comparingInt((ShipyardHit hit) -> DockingEffort.of(hit.stationType()))
                .thenComparing(returnClosest ? byDistance : byPrice.thenComparing(byDistance)));
        return List.copyOf(sorted);
    }
}
