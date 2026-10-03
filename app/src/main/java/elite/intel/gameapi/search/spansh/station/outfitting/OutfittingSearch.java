package elite.intel.gameapi.search.spansh.station.outfitting;

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
 * Where to buy a ship module: the stations whose outfitting stocks it, as Spansh last saw them.
 *
 * <p>The module sibling of the commodity search, on the same endpoint with a different filter. A module is
 * not a market good - it is bought at the outfitting screen and has no supply or demand - so the request
 * asks about the station's module list, and the answer is read from it: every listing at the station that
 * fits what was asked, so a commander who named a size but no class hears which classes are there.
 *
 * <p>WHY price is still a ranking: module prices are fixed by the game, so a station is only ever cheaper
 * because a Power's discount applies there - and that is exactly the station a commander buying a
 * million-credit drive wants to hear about first. Between stations at the same price, nearest wins.
 */
public final class OutfittingSearch {

    /**
     * Only outfitting Spansh has seen this recently is trusted, in its own relative-time vocabulary.
     * Outfitting changes with the background simulation as a station's economy shifts, so a listing
     * older than a week may describe a shop that no longer stocks the module.
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
     * Stations to weigh per request. Each hit carries the station's entire market AND outfitting whether
     * asked for or not - a page of ten measured just under a megabyte - so the cheapest is picked out of
     * the nearest ten rather than out of the whole radius.
     */
    private static final int CANDIDATES = 10;

    private static final Logger log = LogManager.getLogger(OutfittingSearch.class);

    private OutfittingSearch() {
    }

    /**
     * The stations within reach that stock the module, best first, outside the commander's current system.
     *
     * <p>The radius widens as {@link SearchRadii#widening} says rather than report that a module which
     * exists is sold nowhere; the caller compares the winner's distance against what was asked for and
     * says so.
     *
     * @param wanted           the module, with whatever size, class and mount the commander stated
     * @param currentSystem    the system the ship is in, whose stations are never the answer - see
     *                         {@link CurrentSystemFilter}
     * @param maxDistanceLy    the radius to try first
     * @param requiresLargePad when true only stations with a large pad are candidates
     * @param returnClosest    true for the nearest station, false for the cheapest listing
     * @return the stations found, ranked; empty when even the widest sweep found none
     */
    public static List<OutfittingHit> find(WantedModule wanted, double x, double y, double z, String currentSystem,
                                           int maxDistanceLy, boolean requiresLargePad, boolean returnClosest) {
        for (int radius : SearchRadii.widening(maxDistanceLy)) {
            TradeStationSearchCriteria criteria = searchCriteria(wanted, x, y, z, radius, requiresLargePad);
            log.debug("Outfitting search criteria: {}", criteria.toJson());
            TradeStationSearchResultDto response = StationSearchClient.getInstance().searchStations(criteria);
            // A failed POST, a search that times out and an empty body all arrive here as a null.
            if (response == null || response.getResults() == null) continue;
            List<OutfittingHit> found = rank(PermitLockedSystems.reachable(
                    CurrentSystemFilter.exclude(stocking(response.getResults(), wanted), currentSystem)), returnClosest);
            if (!found.isEmpty()) return found;
            log.debug("No outfitting stocking {} within {} ly; widening", wanted, radius);
        }
        return List.of();
    }

    /**
     * The request body.
     *
     * <p>Package-private and separate from the call so the wire shape can be asserted without a live search:
     * Spansh ignores a filter key it does not recognise and matches nothing against a shape it does not
     * expect, so either mistake narrows the search silently instead of failing it - and the commander is
     * simply told nobody sells the module.
     */
    static TradeStationSearchCriteria searchCriteria(
            WantedModule wanted, double x, double y, double z, int radius, boolean requiresLargePad) {

        TradeStationSearchCriteria.StationType stationType = new TradeStationSearchCriteria.StationType();
        // Carriers left out for the reason the commodity search gives: one may have jumped by the time we
        // arrive, and a module is never so rare that only a carrier stocks it.
        stationType.setTypes(TradeStationSearchCriteria.StationType.EVERY_STATIC_TRADE_TYPE);

        /// NOTE: Spansh API is very inconsistent. The light year radius takes a min/max pair of STRINGS and
        /// is silently ignored when sent as a "<=>" range - unlike every other range filter here.
        TradeStationSearchCriteria.Distance distance = new TradeStationSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(radius);

        TradeStationSearchCriteria.OutfittingModule module = new TradeStationSearchCriteria.OutfittingModule(wanted.spellings());
        module.setSize(wanted.size());
        module.setRating(wanted.rating());

        TradeStationSearchCriteria.UpdatedAt seen = new TradeStationSearchCriteria.UpdatedAt();
        seen.setComparison("<=>");
        seen.setValue(List.of(FRESHNESS_WINDOW, "now"));

        TradeStationSearchCriteria.Filters filters = new TradeStationSearchCriteria.Filters();
        filters.setStationType(stationType);
        filters.setDistanceToStarSystem(distance);
        filters.setDistanceToArrival(new TradeStationSearchCriteria.RangeFilter(0, MAX_ARRIVAL_LS));
        filters.setModules(List.of(module));
        filters.setUpdatedAt(seen);
        if (requiresLargePad) {
            filters.setLargePads(new TradeStationSearchCriteria.RangeFilter(1, PADS_MANY));
        }

        TradeStationSearchCriteria criteria = new TradeStationSearchCriteria();
        criteria.setFilters(filters);
        criteria.setReferenceCoords(new TradeStationSearchCriteria.ReferenceCoords(x, y, z));
        // WHY a server-side sort: the page is small, so without it page 0 is ten ARBITRARY matches out of
        // the whole radius and the nearest one is very likely not among them.
        criteria.setSort(List.of(new TradeStationSearchCriteria.DistanceSort()));
        criteria.setSize(CANDIDATES);
        criteria.setPage(0);
        return criteria;
    }

    /**
     * The page as hits: every station whose outfitting actually lists the module, with the listings that
     * fit. Re-checked against what Spansh sent rather than trusted, because the mount is not a filter the
     * endpoint has and a station that answered the name and size may stock only the wrong mount.
     */
    static List<OutfittingHit> stocking(List<TradeStationSearchResultDto.StationResult> stations, WantedModule wanted) {
        List<OutfittingHit> hits = new ArrayList<>();
        for (TradeStationSearchResultDto.StationResult station : stations) {
            if (station.getName() == null || station.getSystemName() == null || station.getModules() == null) continue;
            List<StockedModule> stocked = new ArrayList<>();
            for (TradeStationSearchResultDto.StationResult.Module module : station.getModules()) {
                if (module.getPrice() == null || module.getModuleClass() == null || module.getRating() == null)
                    continue;
                if (!wanted.matches(module.getName(), module.getModuleClass(), module.getRating(), module.getWeaponMode()))
                    continue;
                stocked.add(new StockedModule(module.getName(), module.getModuleClass(), module.getRating(), module.getWeaponMode(), module.getPrice()));
            }
            if (stocked.isEmpty()) continue;
            stocked.sort(Comparator.comparingLong(StockedModule::price));
            hits.add(new OutfittingHit(station.getSystemName(), station.getName(), station.getType(),
                    station.getDistance() == null ? 0 : station.getDistance(), stocked));
        }
        return hits;
    }

    /**
     * Puts the answer the commander asked for at the head: the nearest shop, or the cheapest listing and
     * then the nearest. Easiest to dock at comes before either, for the reason {@link DockingEffort} gives.
     */
    static List<OutfittingHit> rank(List<OutfittingHit> hits, boolean returnClosest) {
        List<OutfittingHit> sorted = new ArrayList<>(hits);
        Comparator<OutfittingHit> byDistance = Comparator.comparingDouble(OutfittingHit::distanceLy);
        sorted.sort(Comparator.comparingInt((OutfittingHit hit) -> DockingEffort.of(hit.stationType()))
                .thenComparing(returnClosest
                        ? byDistance
                        : Comparator.comparingLong(OutfittingHit::cheapest).thenComparing(byDistance)));
        return List.copyOf(sorted);
    }
}
