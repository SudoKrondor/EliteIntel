package elite.intel.gameapi.search.spansh.station.refuel;

import elite.intel.gameapi.search.spansh.station.DockingEffort;
import elite.intel.gameapi.search.spansh.station.StationSearchClient;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.util.ShipPadSizes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Where a ship with no fuel scoop can go to refuel: the nearest station offering the Refuel service that
 * this particular ship can actually land on.
 *
 * <p>WHY this exists next to the commodity search: fuel is not a market good. It is bought at the station
 * services menu, so no commodity filter finds it and no market has it in stock - the question is only
 * whether the station offers {@link #REFUEL_SERVICE}, which Spansh records per station.
 *
 * <p>WHY the pad rule is applied here and not by Spansh: a ship fits any pad its own size or bigger, so a
 * medium ship wants "medium OR large", and Spansh's three pad counts are separate filters that AND together.
 * See {@link #padFilters}: a large ship is one search, a medium ship is two searches merged, and a small ship
 * needs no pad filter at all because every dockable station has some pad it fits on. The counts are then
 * re-checked on each hit through {@link ShipPadSizes#canDock}, which is what actually decides.
 *
 * <p>WHY fleet carriers are not offered: a carrier's recorded position is only where Spansh last saw it and
 * its owner can switch docking or refuelling off at will. A commander short of fuel is the last person who
 * can afford to jump somewhere and find nothing there, so only stations that stay put are named.
 */
public final class RefuelStationSearch {

    /**
     * Spansh's own name for the refuelling service, from {@code /api/stations/field_values/services}.
     * Matched exactly: a name Spansh does not know matches no station rather than failing the search.
     */
    public static final String REFUEL_SERVICE = "Refuel";

    /**
     * Comfortably above any station's pad count; each pad range is really "one or more".
     */
    private static final int PADS_MANY = 100;

    /**
     * Human space is roughly this wide around Sol, so a last sweep this far covers every fixed station there
     * is. Reached only when the radius the commander asked for, and double it, came back empty.
     */
    private static final int INHABITED_BUBBLE_LY = 1000;

    /**
     * How far from the arrival point a station may sit and still count as somewhere to refuel. Supercruise
     * burns no fuel, so this is a limit on the commander's evening rather than on their tank - but a station
     * a quarter of a million light seconds out is half an hour of flying, and there is always another.
     */
    private static final int MAX_ARRIVAL_LS = 25_000;

    /**
     * Stations to weigh per request. Each Spansh station result carries the station's entire market whether
     * we asked for it or not (~65 KB apiece), so this is a page size paid for in megabytes.
     */
    private static final int CANDIDATES = 15;

    private static final Logger log = LogManager.getLogger(RefuelStationSearch.class);

    private RefuelStationSearch() {
    }

    /**
     * The stations within reach that sell fuel, best candidate first.
     *
     * <p>The radius widens twice - to double the stated one, then to the whole inhabited bubble - rather than
     * report that there is nowhere to refuel. A commander who cannot reach the answer still needs to be told
     * where it is; the caller compares the winner's distance against what was asked for and says so.
     *
     * @param maxDistanceLy the radius to try first, normally the ship's jump range
     * @param padSize       the pad the ship needs, as {@link ShipPadSizes#getPadSize} spells it
     * @return the stations found, ranked; empty when even the widest sweep found none
     */
    public static List<RefuelStation> nearest(double x, double y, double z, int maxDistanceLy, String padSize) {
        for (int radius : radiiToTry(maxDistanceLy)) {
            List<RefuelStation> found = rank(fetch(x, y, z, radius, padSize), padSize);
            if (!found.isEmpty()) return found;
            log.debug("No refuelling station a {} ship can use within {} ly; widening", padSize, radius);
        }
        return List.of();
    }

    /**
     * The radii to try, in order. Each is only reached when the one before it found nothing, so a commander
     * inside the bubble still gets a single round trip.
     * <p>
     * Public because a caller reporting that nothing was found has to say how far the search actually looked,
     * which is the last rung and not the radius it was given.
     */
    public static List<Integer> radiiToTry(int maxDistanceLy) {
        // Guarded so a commander who asks for a galaxy-wide radius does not overflow it into a negative one.
        int widened = maxDistanceLy > Integer.MAX_VALUE / 2 ? maxDistanceLy : maxDistanceLy * 2;
        List<Integer> radii = new ArrayList<>();
        radii.add(maxDistanceLy);
        if (widened > maxDistanceLy) radii.add(widened);
        // Never NARROWS an already-wide ask: a commander who said 2000 ly keeps his 4000 ly second sweep.
        if (INHABITED_BUBBLE_LY > widened) radii.add(INHABITED_BUBBLE_LY);
        return List.copyOf(radii);
    }

    /**
     * One radius, over every pad filter this ship needs, de-duplicated. A station reached by two of the
     * searches - a large-pad starport also has medium pads - is one station, and the first sighting wins.
     */
    private static List<TradeStationSearchResultDto.StationResult> fetch(
            double x, double y, double z, int radius, String padSize) {

        Map<String, TradeStationSearchResultDto.StationResult> byId = new LinkedHashMap<>();
        for (PadFilter padFilter : padFilters(padSize)) {
            TradeStationSearchCriteria criteria = searchCriteria(x, y, z, radius, padFilter);
            log.debug("Refuel station criteria: {}", criteria.toJson());
            TradeStationSearchResultDto response = StationSearchClient.getInstance().searchStations(criteria);
            // A failed POST, a search that times out and an empty body all arrive here as a null.
            if (response == null || response.getResults() == null) continue;
            for (TradeStationSearchResultDto.StationResult station : response.getResults()) {
                byId.putIfAbsent(identity(station), station);
            }
        }
        return List.copyOf(byId.values());
    }

    /**
     * Which pad count a request constrains. A search is one of these, and a ship needs one search per entry
     * {@link #padFilters} hands back.
     */
    enum PadFilter {
        /**
         * No pad constraint: every dockable station has a pad a small ship fits on.
         */
        ANY,
        /**
         * At least one medium pad.
         */
        MEDIUM,
        /**
         * At least one large pad.
         */
        LARGE
    }

    /**
     * The pad filters to search under, one request each.
     *
     * <p>A large ship needs a large pad and nothing else will do, so that is one search. A medium ship fits a
     * medium pad or a large one, and Spansh will not answer an OR: measured live, there are stations with a
     * large pad and no medium pad at all, so asking only about medium pads would hide them. Hence two
     * searches, merged. A small ship fits every pad there is, so it asks for no pad filter and sees
     * everything.
     */
    static List<PadFilter> padFilters(String padSize) {
        if (ShipPadSizes.LARGE.equals(padSize)) return List.of(PadFilter.LARGE);
        if (ShipPadSizes.MEDIUM.equals(padSize)) return List.of(PadFilter.MEDIUM, PadFilter.LARGE);
        return List.of(PadFilter.ANY);
    }

    /**
     * "At least one pad of this size", the only pad constraint any of these searches has a use for. Spansh
     * exposes no boolean, so it is written as a range nothing can exceed.
     */
    private static TradeStationSearchCriteria.RangeFilter atLeastOnePad() {
        return new TradeStationSearchCriteria.RangeFilter(1, PADS_MANY);
    }

    /**
     * The request body.
     *
     * <p>Package-private and separate from the call so the wire shape can be asserted without a live search:
     * Spansh ignores a filter key it does not recognise, and matches nothing at all against a service or
     * station type it does not know, so either mistake narrows the search silently instead of failing it -
     * and the commander is simply told there is nowhere to refuel.
     *
     * @param padFilter which pad count this request constrains; see {@link #padFilters}
     */
    static TradeStationSearchCriteria searchCriteria(
            double x, double y, double z, int radius, PadFilter padFilter) {

        TradeStationSearchCriteria.StationType stationType = new TradeStationSearchCriteria.StationType();
        stationType.setTypes(TradeStationSearchCriteria.StationType.EVERY_STATIC_TRADE_TYPE);

        /// NOTE: Spansh API is very inconsistent. The light year radius takes a min/max pair of STRINGS and
        /// is silently ignored when sent as a "<=>" range - unlike every other range filter here.
        TradeStationSearchCriteria.Distance distance = new TradeStationSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(radius);

        TradeStationSearchCriteria.Filters filters = new TradeStationSearchCriteria.Filters();
        filters.setStationType(stationType);
        filters.setDistanceToStarSystem(distance);
        filters.setDistanceToArrival(new TradeStationSearchCriteria.RangeFilter(0, MAX_ARRIVAL_LS));
        filters.setServices(List.of(new TradeStationSearchCriteria.Service(List.of(REFUEL_SERVICE))));
        if (padFilter == PadFilter.MEDIUM) filters.setMediumPads(atLeastOnePad());
        if (padFilter == PadFilter.LARGE) filters.setLargePads(atLeastOnePad());

        TradeStationSearchCriteria criteria = new TradeStationSearchCriteria();
        criteria.setFilters(filters);
        criteria.setReferenceCoords(new TradeStationSearchCriteria.ReferenceCoords(x, y, z));
        criteria.setSort(List.of(new TradeStationSearchCriteria.DistanceSort()));
        criteria.setSize(CANDIDATES);
        criteria.setPage(0);
        return criteria;
    }

    /**
     * The stations this ship can actually land on, in the order a commander would pick between them:
     * easiest to dock at first, then nearest, then shortest supercruise once there.
     *
     * <p>The pad check is repeated here over the counts Spansh reported, because the filters above are a
     * SEARCH - two of them merged, for a medium ship - and merging two searches is exactly how a station
     * that satisfied neither could otherwise slip through.
     */
    static List<RefuelStation> rank(List<TradeStationSearchResultDto.StationResult> stations, String padSize) {
        List<RefuelStation> usable = new ArrayList<>();
        for (TradeStationSearchResultDto.StationResult station : stations) {
            if (station.getName() == null || station.getSystemName() == null) continue;
            if (!ShipPadSizes.canDock(padSize, count(station.getSmallPads()),
                    count(station.getMediumPads()), count(station.getLargePads()))) {
                continue;
            }
            usable.add(new RefuelStation(
                    station.getSystemName(), station.getName(), station.getType(),
                    station.getDistance() == null ? 0 : station.getDistance(),
                    station.getDistanceToArrival() == null ? 0 : station.getDistanceToArrival()));
        }

        usable.sort(Comparator.comparingInt((RefuelStation found) -> DockingEffort.of(found.stationType()))
                .thenComparingDouble(RefuelStation::distanceLy)
                .thenComparingDouble(RefuelStation::arrivalLs));
        return List.copyOf(usable);
    }

    /**
     * A missing pad count reads as no pads of that size. Spansh sends all three for every station; a row
     * that somehow arrives without them is not one to send a commander short of fuel to.
     */
    private static int count(Integer pads) {
        return pads == null ? 0 : pads;
    }

    /**
     * Spansh's station id, falling back to the name pair when a row has none - the merge only needs to
     * recognise the same station twice, and two stations never share both a name and a system.
     */
    private static String identity(TradeStationSearchResultDto.StationResult station) {
        return station.getId() != null ? station.getId() : station.getSystemName() + "|" + station.getName();
    }
}
