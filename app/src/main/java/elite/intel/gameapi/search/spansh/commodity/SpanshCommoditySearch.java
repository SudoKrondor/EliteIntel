package elite.intel.gameapi.search.spansh.commodity;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.search.spansh.station.StationSearchClient;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchCriteria;
import elite.intel.gameapi.search.spansh.station.marketstation.TradeStationSearchResultDto;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static elite.intel.util.StringUtls.localizedEventPlural;

/**
 * Finds the markets selling a commodity, through one Spansh stations search.
 * <p>
 * WHY one search and not a scan: this used to walk EDSM - every system in range, then every station in
 * every system, then every market in every station, filtering the commodity out in Java. Spansh answers
 * the same question in a single request because its stations index already carries the markets, and its
 * {@code marketplace} filter asks about stock and price on the commodity itself.
 * <p>
 * WHY the page is asked for in distance order even when the commander wants the best price: Spansh cannot
 * sort by the price of one commodity (a {@code buy_price} sort is accepted and quietly ignored), so the
 * cheapest is picked here, out of the nearest {@link #BEST_PRICE_CANDIDATES}. Trading the whole radius for
 * the near part of it is deliberate - the saving on a hold of cargo is rarely worth a hundred extra jumps.
 */
public final class SpanshCommoditySearch {

    /**
     * Comfortably above any station's large-pad count; the pad range is really "one or more".
     */
    private static final int LARGE_PADS_MANY = 100;
    /**
     * Upper end of an otherwise open range. Spansh has no "at least" comparison that works - see
     * {@link TradeStationSearchCriteria.Marketplace} - so an unbounded ask is written as a range that
     * nothing can exceed.
     */
    private static final int UNBOUNDED = Integer.MAX_VALUE;
    /**
     * The market has to be selling, not merely listing the good.
     */
    private static final int SELLING_AT_ALL = 1;
    /**
     * Stations to weigh for price. Each result carries the station's ENTIRE market (~50 KB of it), so this
     * is a page size paid for in megabytes, not a free widening of the search.
     */
    private static final int BEST_PRICE_CANDIDATES = 50;
    /**
     * Stations to fetch when only the nearest one is wanted. The page comes back in distance order, so the
     * answer is the first row; the rest are here only so a station rejected below has a successor.
     */
    private static final int NEAREST_CANDIDATES = 10;

    /**
     * Words an English title leaves in lower case, and so does Spansh: "Fruit and Vegetables", never
     * "Fruit And Vegetables" - which matches nothing at all.
     */
    private static final Set<String> MINOR_WORDS = Set.of("and", "of", "the", "in", "a", "an", "to", "for", "on");

    private static final Logger log = LogManager.getLogger(SpanshCommoditySearch.class);

    private SpanshCommoditySearch() {
    }

    /**
     * Markets within {@code maxDistanceLy} selling {@code commodityToFind}, best candidate first: nearest
     * when {@code returnClosest}, cheapest otherwise.
     *
     * @param commodityToFind the commodity by its English name; capitalisation is forgiven, see {@link #spellings}
     * @param refStarSystem   the system distances are measured from
     * @param maxDistanceLy   search radius in light years
     * @param profile         the ship's trade profile - hold size, arrival distance, pad and station rules
     */
    public static List<CommoditySearchResult> search(
            String commodityToFind, String refStarSystem, int maxDistanceLy,
            TradeRouteSearchCriteria profile, boolean returnClosest) {

        TradeStationSearchCriteria criteria = searchCriteria(commodityToFind, refStarSystem, maxDistanceLy, profile, returnClosest);
        log.debug("Commodity search criteria: {}", criteria.toJson());

        TradeStationSearchResultDto response = StationSearchClient.getInstance().searchStations(criteria);
        // A failed POST, a search that times out and an empty body all arrive here as a null.
        List<TradeStationSearchResultDto.StationResult> stations =
                response == null || response.getResults() == null ? List.of() : response.getResults();

        GameEventBus.publish(new MissionCriticalAnnouncementEvent(
                localizedEventPlural(stations.size(), "event.search.commodity.marketsFound")));

        return rank(stations, commodityToFind, returnClosest);
    }

    /**
     * Puts the answer the commander asked for at the head of the list: the nearest market, or the one
     * selling cheapest.
     * <p>
     * Separate from the call because the ORDER is the answer - the caller flies to {@code getFirst()} and
     * never looks at the rest - and because Spansh hands the page back in distance order regardless, so
     * a best-price search that forgot to re-sort would look entirely correct in the log.
     */
    static List<CommoditySearchResult> rank(
            List<TradeStationSearchResultDto.StationResult> stations, String commodityToFind, boolean returnClosest) {

        List<CommoditySearchResult> results = new ArrayList<>();
        for (TradeStationSearchResultDto.StationResult station : stations) {
            TradeStationSearchResultDto.StationResult.MarketEntry entry = sellsIt(station, commodityToFind);
            if (entry == null) continue;
            results.add(asResult(station, entry));
        }

        results.sort(returnClosest
                ? Comparator.comparingDouble(CommoditySearchResult::getDistanceFromPlayer)
                : Comparator.comparingDouble(CommoditySearchResult::getPrice));
        return results;
    }

    /**
     * The request body.
     * <p>
     * Package-private and separate from the call so the wire shape can be asserted without a live search:
     * Spansh ignores a filter key it does not recognise and matches nothing at all against a value it does
     * not know, so either mistake narrows the search silently instead of failing it, and the commander just
     * hears that the commodity is nowhere to be found.
     */
    static TradeStationSearchCriteria searchCriteria(
            String commodityToFind, String refStarSystem, int maxDistanceLy,
            TradeRouteSearchCriteria profile, boolean returnClosest) {

        List<String> stationTypes = new ArrayList<>(TradeStationSearchCriteria.StationType.ORBITAL_TRADE_TYPES);
        if (profile.isAllowPlanetary())
            stationTypes.addAll(TradeStationSearchCriteria.StationType.PLANETARY_TRADE_TYPES);
        if (profile.isAllowFleetCarriers())
            stationTypes.addAll(TradeStationSearchCriteria.StationType.CARRIER_TRADE_TYPES);
        TradeStationSearchCriteria.StationType stationType = new TradeStationSearchCriteria.StationType();
        stationType.setTypes(stationTypes);

        /// NOTE: Spansh API is very inconsistent. The light year radius takes a min/max pair of STRINGS and
        /// is silently ignored when sent as a "<=>" range - unlike every other range filter here.
        TradeStationSearchCriteria.Distance distance = new TradeStationSearchCriteria.Distance();
        distance.setMin(0);
        distance.setMax(maxDistanceLy);

        // A hold's worth in stock, or the commander flies out for a part load. Guarded because a range
        // starting at zero would also match markets that list the good and have none.
        int holdFull = Math.max(1, profile.getMaxCargo());
        TradeStationSearchCriteria.Marketplace marketplace = new TradeStationSearchCriteria.Marketplace(spellings(commodityToFind));
        marketplace.setSupply(new TradeStationSearchCriteria.RangeFilter(holdFull, UNBOUNDED));
        marketplace.setBuyPrice(new TradeStationSearchCriteria.RangeFilter(SELLING_AT_ALL, UNBOUNDED));

        TradeStationSearchCriteria.Filters filters = new TradeStationSearchCriteria.Filters();
        filters.setStationType(stationType);
        filters.setDistanceToStarSystem(distance);
        filters.setDistanceToArrival(new TradeStationSearchCriteria.RangeFilter(0, profile.getMaxLsFromArrival()));
        filters.setServices(List.of(new TradeStationSearchCriteria.Service(List.of(TradeStationSearchCriteria.MARKET_SERVICE))));
        filters.setMarketplace(List.of(marketplace));
        if (profile.isRequiresLargePad()) {
            filters.setLargePads(new TradeStationSearchCriteria.RangeFilter(1, LARGE_PADS_MANY));
        }

        TradeStationSearchCriteria criteria = new TradeStationSearchCriteria();
        criteria.setFilters(filters);
        criteria.setReferenceSystem(refStarSystem);
        criteria.setSort(List.of(new TradeStationSearchCriteria.DistanceSort()));
        criteria.setSize(returnClosest ? NEAREST_CANDIDATES : BEST_PRICE_CANDIDATES);
        criteria.setPage(0);
        return criteria;
    }

    /**
     * The commodity name in every capitalisation Spansh might be holding it under.
     * <p>
     * WHY more than one: Spansh matches the name EXACTLY - "gold" and "Fruit And Vegetables" both return
     * zero stations where "Gold" and "Fruit and Vegetables" return thousands - and our own commodity table
     * disagrees with Spansh's spelling on a handful of goods ("Liquid Oxygen" against Spansh's "Liquid
     * oxygen", "Eden Apples Of Aerial" against "Eden Apples of Aerial"). Sending the alternatives costs
     * nothing: the filter ORs the list and a name Spansh does not know contributes no stations rather than
     * poisoning the query - measured live, {@code ["Gold", "Nonexistent Widget"]} returns the Gold markets.
     * <p>
     * The name we were given always leads, because for 389 of the 398 goods Spansh sells it is already
     * right, and the variants are only ever guesses at the rest.
     */
    static List<String> spellings(String commodity) {
        String lower = commodity.toLowerCase();
        List<String> spellings = new ArrayList<>();
        for (String spelling : List.of(commodity, titleCase(lower, false), titleCase(lower, true), sentenceCase(lower))) {
            if (!spellings.contains(spelling)) spellings.add(spelling);
        }
        return spellings;
    }

    /**
     * "liquid oxygen" as "Liquid Oxygen", or as "The Waters of Shintara" when {@code keepMinorWordsLower} -
     * English titles leave "and", "of" and "the" in lower case, and Spansh's names follow suit.
     */
    private static String titleCase(String lower, boolean keepMinorWordsLower) {
        String[] words = lower.split(" ");
        StringBuilder titled = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) titled.append(" ");
            titled.append(i > 0 && keepMinorWordsLower && MINOR_WORDS.contains(words[i]) ? words[i] : capitalised(words[i]));
        }
        return titled.toString();
    }

    /**
     * "liquid oxygen" as "Liquid oxygen" - Spansh spells a few goods this way.
     */
    private static String sentenceCase(String lower) {
        return capitalised(lower);
    }

    private static String capitalised(String word) {
        return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * The station's market entry for the commodity, or null when it has none.
     * <p>
     * Belt and braces over the {@code marketplace} filter, which has already answered this question - but
     * the entry is where the price and the exact commodity name come from, so it has to be found anyway,
     * and a row that somehow arrives without one is dropped rather than reported at a price of zero.
     */
    private static TradeStationSearchResultDto.StationResult.MarketEntry sellsIt(
            TradeStationSearchResultDto.StationResult station, String commodity) {
        if (station.getMarket() == null) return null;
        return station.getMarket().stream()
                .filter(entry -> commodity.equalsIgnoreCase(entry.getCommodity()))
                .filter(entry -> entry.getBuyPrice() != null && entry.getBuyPrice() > 0)
                .findFirst()
                .orElse(null);
    }

    private static CommoditySearchResult asResult(
            TradeStationSearchResultDto.StationResult station,
            TradeStationSearchResultDto.StationResult.MarketEntry entry) {
        CommoditySearchResult result = new CommoditySearchResult();
        result.setCommodity(entry.getCommodity());
        result.setPrice(entry.getBuyPrice());   // buy_price is what the commander pays
        result.setStarSystem(station.getSystemName());
        result.setStationName(station.getName());
        result.setStationType(station.getType());
        result.setDistanceFromPlayer(station.getDistance() == null ? 0 : station.getDistance());
        return result;
    }
}
