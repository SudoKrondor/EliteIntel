package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.CommoditySearchResultDao.FoundLine;
import elite.intel.db.dao.CommoditySearchResultDao.FoundMarket;
import elite.intel.db.managers.CommoditySearchResultManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.commodity.BasketResult;
import elite.intel.gameapi.search.spansh.commodity.CommoditySearchResult;
import elite.intel.gameapi.search.spansh.commodity.SpanshCommoditySearch;
import elite.intel.gameapi.search.spansh.commodity.WantedCommodity;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * "Where do I buy this, and take me there" - shared by the commander asking for a commodity by name
 * and by the mission board asking on their behalf.
 * <p>
 * Both arrive at the same place: a commodity spelled the way the commodities table spells it, which is
 * the way Spansh matches it. Everything after that - the trade profile checks, the search, the spoken
 * answer, the reminder and the route - is the same job, and the two commands running slightly different
 * versions of it is how one of them quietly stops honouring the profile.
 */
final class CommodityPurchaseSearch {

    private static final Logger log = LogManager.getLogger(CommodityPurchaseSearch.class);

    /**
     * Human space is about this wide around Sol; the fallback radius when the ship's jump range is unknown.
     */
    private static final int INHABITED_BUBBLE_LY = 1000;

    private CommodityPurchaseSearch() {
    }

    /**
     * The radius to search when the commander names none: twice what the ship can jump, which scales the
     * search to the ship actually flying it.
     * <p>
     * Falls back to the inhabited bubble - roughly {@value #INHABITED_BUBBLE_LY} ly around Sol, and the same
     * figure the sibling ring search defaults to - when the loadout has no FSD range yet. Doubling an unknown
     * jump range gives zero, and a zero radius finds nothing however many times it is widened, so the
     * commander would be told the good does not exist anywhere.
     */
    static int defaultRange() {
        int shipRange = (int) PlayerSession.getInstance().getShipLoadout().getMaxJumpRange() * 2;
        return shipRange < 1 ? INHABITED_BUBBLE_LY : shipRange;
    }

    /**
     * Tonnes the ship can carry, or 0 when the game has not yet said which ship we are in.
     * <p>
     * Shared so callers that size a load themselves - collecting from the commander's own carrier, say -
     * measure it against the same hold the market search does.
     */
    static int holdCapacity() {
        TradeRouteSearchCriteria criteria = TradeProfileManager.getInstance().getCriteria(false);
        return criteria == null ? 0 : criteria.getMaxCargo();
    }

    /**
     * Finds the market to buy a hold's worth of {@code commodity} at and plots a route to it.
     * <p>
     * For the commander who simply names a good, which is a request to fill the ship. A caller working from
     * a standing list - a construction manifest, a mission board - wants {@link #findBasketAndPlot} instead:
     * it knows how much of each good is actually needed, and can load several of them in one run.
     *
     * @param commodity     the commodity in the commodities table's own spelling; Spansh matches it exactly,
     *                      so title-casing it first quietly breaks the 23 goods with punctuation in them
     * @param distance      search radius in light years
     * @param returnClosest true for the nearest market, false for the best price
     * @return a localized message when the search could not be run, found nothing, or ended without a new
     * route being plotted; null when the answer has already been spoken and the route is under way
     */
    static String findAndPlot(String commodity, int distance, boolean returnClosest) {
        TradeProfileManager tradeProfileManager = TradeProfileManager.getInstance();
        String starName = PlayerSession.getInstance().getPrimaryStarName();

        String searchMode = StringUtls.localizedResponse(returnClosest ? "handler.commodity.modeNearest" : "handler.commodity.modeBest");
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.commodity.searching", searchMode, commodity, distance), false);
        TradeRouteSearchCriteria criteria = tradeProfileManager.getCriteria(false);
        // Null until the game has told us which ship we are in, and a ship is what carries the hold the
        // search sizes itself to.
        if (criteria == null) {
            return StringUtls.localizedResponse("handler.commodity.noCargoCapacity");
        }
        int cargoCapacity = criteria.getMaxCargo();
        if (cargoCapacity == 0) {
            return StringUtls.localizedResponse("handler.commodity.noCargoCapacity");
        }
        if (criteria.getMaxLsFromArrival() == 0) {
            return StringUtls.localizedResponse("handler.commodity.maxDistanceFromArrivalNoSet");
        }
        // Nobody said how many, so this trip is for a hold's worth.
        int wanted = cargoCapacity;
        List<CommoditySearchResult> results = SpanshCommoditySearch.search(
                commodity,
                starName,
                distance,
                criteria,
                returnClosest,
                wanted
        );
        if (results.isEmpty()) {
            return StringUtls.localizedResponse("handler.commodity.noMatch");
        }
        CommoditySearchResult result = results.getFirst();
        // A carrier is only ever offered when nothing that stays put sells the good, and it jumps - so the
        // commander is told, in the spoken line AND in the reminder he will read again on arrival.
        String reminder = StringUtls.localizedResponse(
                result.isFleetCarrier() ? "handler.commodity.headToCarrier" : "handler.commodity.headTo",
                result.getStarSystem(), result.getStationName(), result.getStationType(), result.getPrice());
        // The search doubles the radius rather than call a good nonexistent, so when the answer lies outside
        // what the commander asked for he is told - being sent 340 ly after asking for 100 is worth a
        // sentence, and silently substituting a different question for his is not.
        if (result.getDistanceFromPlayer() > distance) {
            reminder += " " + StringUtls.localizedResponse("handler.commodity.beyondRange",
                    distance, Math.round(result.getDistanceFromPlayer()));
        }
        // The search settles for a part load rather than call an ordinary good nonexistent, so when it has
        // it says how much is there - otherwise the shortfall is discovered at the commodity market. Buying
        // what is there is still progress: the next search asks for whatever is left after this stop.
        if (result.getSupply() > 0 && result.getSupply() < wanted) {
            reminder += " " + StringUtls.localizedResponse("handler.commodity.partLoad", result.getSupply());
        }
        CompanionRuntime.narrator().filler(reminder, false);
        ReminderManager.getInstance().setReminder(reminder, result.getStarSystem(), result.getStationName(), null);
        // The reminder is prose for the voice; the stock and the unit price are inside that sentence and
        // nowhere a HUD card can read them. Store the result itself so the overlay can show what was found
        // - and keep showing it after a restart mid-trip, which is what the overlay's derive-never-remember
        // rule is for.
        recordForOverlay(result);

        // The plotter's own outcome, not a discarded return. It declines to re-plot a route the commander is
        // already flying, and swallowing that left them hearing "head to Fairfax Landing", seeing no galaxy
        // map open, and concluding the search had failed - when the ship was already pointed at that system.
        return new RoutePlotter().plotRoute(result.getStarSystem());
    }

    /**
     * Finds the market that can fill the most of a shopping list and plots a route to it.
     * <p>
     * The multi-commodity sibling of {@link #findAndPlot}, for a caller holding a standing list of goods
     * rather than one request: a colonisation manifest, or a stack of source-and-return missions. Its whole
     * reason for existing is the long tail of such a list - nine commodities of sixty tonnes each, which
     * one at a time is nine round trips with the hold empty for eight of them.
     * <p>
     * Falls back to exactly {@link #findAndPlot}'s behaviour whenever the list has one entry, or when the
     * best market turns out to stock only the anchor. The commander is then told the same thing they were
     * told before, because nothing more is true.
     *
     * @param wanted what is still needed, in the caller's priority order, anchor first
     * @return a localized message when the search could not be run, found nothing, or ended without a new
     * route being plotted; null when the answer has already been spoken and the route is under way
     */
    static String findBasketAndPlot(List<WantedCommodity> wanted, int distance, boolean returnClosest) {
        if (wanted == null || wanted.isEmpty()) {
            return StringUtls.localizedResponse("handler.commodity.noMatch");
        }
        WantedCommodity anchor = wanted.getFirst();

        TradeRouteSearchCriteria criteria = TradeProfileManager.getInstance().getCriteria(false);
        // Null until the game has told us which ship we are in, and a ship is what carries the hold the
        // search sizes itself to.
        if (criteria == null || criteria.getMaxCargo() == 0) {
            return StringUtls.localizedResponse("handler.commodity.noCargoCapacity");
        }
        if (criteria.getMaxLsFromArrival() == 0) {
            return StringUtls.localizedResponse("handler.commodity.maxDistanceFromArrivalNoSet");
        }

        String searchMode = StringUtls.localizedResponse(
                returnClosest ? "handler.commodity.modeNearest" : "handler.commodity.modeBest");
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse(
                "handler.commodity.searching", searchMode, anchor.commodity(), distance), false);

        List<BasketResult> markets = SpanshCommoditySearch.searchBasket(
                wanted, PlayerSession.getInstance().getPrimaryStarName(), distance, criteria, returnClosest,
                criteria.getMaxCargo());
        if (markets.isEmpty()) {
            return StringUtls.localizedResponse("handler.commodity.noMatch");
        }

        BasketResult market = markets.getFirst();
        String reminder = spokenAnswer(market, distance);
        CompanionRuntime.narrator().filler(reminder, false);
        ReminderManager.getInstance().setReminder(reminder, market.starSystem(), market.stationName(), null);
        recordForOverlay(market);

        // As above: whatever the plotter has to say is the commander's answer, not something to drop.
        return new RoutePlotter().plotRoute(market.starSystem());
    }

    /**
     * What the commander is told: where to go, and what to load while they are there.
     * <p>
     * The goods are named in the commander's language, not the commodities table's - the table spells them
     * the way Spansh matches them, which is English whatever the commander speaks.
     */
    private static String spokenAnswer(BasketResult market, int distance) {
        String goods = market.lines().stream()
                .map(line -> StringUtls.localizedResponse("handler.commodity.basketItem",
                        line.unitsToBuy(), FuzzySearch.localizedCommodityName(line.commodity())))
                .collect(Collectors.joining(", "));

        String answer = StringUtls.localizedResponse(
                market.fleetCarrier() ? "handler.commodity.basketAtCarrier" : "handler.commodity.basketAt",
                market.starSystem(), market.stationName(), market.stationType(), goods, market.totalUnits());
        // The search doubles the radius rather than call a good nonexistent, so when the answer lies outside
        // what the commander asked for he is told - being sent 340 ly after asking for 100 is worth a
        // sentence, and silently substituting a different question for his is not.
        if (market.distanceFromPlayer() > distance) {
            answer += " " + StringUtls.localizedResponse("handler.commodity.beyondRange",
                    distance, Math.round(market.distanceFromPlayer()));
        }
        return answer;
    }

    /**
     * Saves the market the search settled on, for the overlay card.
     */
    private static void recordForOverlay(CommoditySearchResult result) {
        FoundMarket found = new FoundMarket();
        found.setCommodity(result.getCommodity());
        found.setStarSystem(result.getStarSystem());
        found.setStationName(result.getStationName());
        found.setStationType(result.getStationType());
        found.setPrice(Math.round(result.getPrice()));
        found.setSupply(result.getSupply());
        found.setFleetCarrier(result.isFleetCarrier());

        FoundLine line = new FoundLine();
        line.setCommodity(result.getCommodity());
        line.setPrice(Math.round(result.getPrice()));
        line.setSupply(result.getSupply());
        // Nobody said how many were wanted, so the card shows what is there rather than a made-up figure.
        line.setUnitsToBuy(0);

        store(found, List.of(line));
    }

    /**
     * As {@link #recordForOverlay(CommoditySearchResult)}, for the whole shopping list.
     */
    private static void recordForOverlay(BasketResult market) {
        FoundMarket found = new FoundMarket();
        // The headline good: the one the search was anchored on and the one the reminder is about.
        found.setCommodity(market.anchor().commodity());
        found.setStarSystem(market.starSystem());
        found.setStationName(market.stationName());
        found.setStationType(market.stationType());
        found.setPrice(market.anchor().price());
        found.setSupply(market.anchor().supply());
        found.setFleetCarrier(market.fleetCarrier());

        List<FoundLine> lines = market.lines().stream().map(source -> {
            FoundLine line = new FoundLine();
            line.setCommodity(source.commodity());
            line.setSymbol(source.symbol());
            line.setPrice(source.price());
            line.setSupply(source.supply());
            line.setUnitsToBuy(source.unitsToBuy());
            return line;
        }).toList();

        store(found, lines);
    }

    /**
     * Best-effort: the commander has already been told where to go and the route is about to be plotted, so
     * a failure to write a display record must not turn a successful search into an error.
     */
    private static void store(FoundMarket found, List<FoundLine> lines) {
        try {
            found.setFoundAt(Instant.now().toString());
            CommoditySearchResultManager.getInstance().save(found, lines);
        } catch (RuntimeException e) {
            log.warn("Could not record the commodity search result for the overlay", e);
        }
    }
}
