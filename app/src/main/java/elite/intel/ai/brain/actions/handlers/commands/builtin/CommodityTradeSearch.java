package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.CommoditySearchResultDao.FoundLine;
import elite.intel.db.dao.CommoditySearchResultDao.FoundMarket;
import elite.intel.db.managers.CommodityMeanPriceManager;
import elite.intel.db.managers.CommoditySearchResultManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.commodity.*;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.session.DockedMarket;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * "Where do I trade this, and take me there" - shared by the commander asking for a commodity by name, by
 * the mission board asking on their behalf, and by the commander asking where to unload what is already in
 * the hold.
 * <p>
 * All of them arrive at the same place: a commodity spelled the way the commodities table spells it, which
 * is the way Spansh matches it. Everything after that - the trade profile checks, the search, the spoken
 * answer, the reminder, the overlay card and the route - is the same job, and the callers running slightly
 * different versions of it is how one of them quietly stops honouring the profile.
 * <p>
 * Buying and selling differ only in which side of the counter is read; see {@link TradeSide}.
 */
final class CommodityTradeSearch {

    private static final Logger log = LogManager.getLogger(CommodityTradeSearch.class);

    /**
     * Human space is about this wide around Sol; the fallback radius when the ship's jump range is unknown.
     */
    private static final int INHABITED_BUBBLE_LY = 1000;

    /**
     * Inside this much of the galactic average the price is not worth a percentage: a market two percent off
     * the mean is an ordinary market, and saying so in figures reads as precision the number does not carry.
     */
    private static final int AVERAGE_ENOUGH_PERCENT = 3;

    /**
     * Older than this and a Spansh price is worth flagging rather than quoting flat. Market prices move as
     * demand is consumed, so a week-old figure is a starting point and not a quote.
     */
    private static final int STALE_AFTER_DAYS = 7;

    private CommodityTradeSearch() {
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
        List<CommoditySearchResult> elsewhere = awayFromThisPad(results);
        if (elsewhere.isEmpty()) {
            return StringUtls.localizedResponse("handler.commodity.onlyMarketIsHere");
        }
        CommoditySearchResult result = elsewhere.getFirst();
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
        reminder += againstGalacticAverage(commodity, result.getPrice());
        reminder += howOldThatPriceIs(result);
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
     * Finds the market paying best for {@code commodity} and plots a route to it.
     * <p>
     * The mirror of {@link #findAndPlot}: same radius default, same escalation ladder, same reminder and
     * card and route. What changes is the side of the counter - the search wants DEMAND rather than stock,
     * and "best price" means the most credits a tonne rather than the fewest.
     * <p>
     * <b>How much is looked for.</b> Whatever is in the hold, because that is what the commander is trying
     * to unload: a market wanting 40 tonnes is no answer for the 300 aboard. A commander who is not carrying
     * the good is planning rather than unloading, so the ask falls back to a hold's worth - the same figure
     * the buy search uses. Either way the ladder settles for a part sale rather than report that nobody
     * wants the good, and says so.
     *
     * @param commodity     the commodity in the commodities table's own spelling; Spansh matches it exactly
     * @param distance      search radius in light years
     * @param returnClosest true for the nearest buyer, false for the best price
     * @return a localized message when the search could not be run, found nothing, or ended without a new
     * route being plotted; null when the answer has already been spoken and the route is under way
     */
    static String findSaleAndPlot(String commodity, int distance, boolean returnClosest) {
        String starName = PlayerSession.getInstance().getPrimaryStarName();

        String searchMode = StringUtls.localizedResponse(
                returnClosest ? "handler.commodity.modeNearest" : "handler.commodity.modeBest");
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse(
                "handler.commodity.searchingBuyer", searchMode, commodity, distance), false);

        TradeRouteSearchCriteria criteria = TradeProfileManager.getInstance().getCriteria(false);
        // Null until the game has told us which ship we are in. The profile is what says how far off the
        // arrival point the ship will go and whether it needs a large pad - as true of unloading as loading.
        if (criteria == null || criteria.getMaxCargo() == 0) {
            return StringUtls.localizedResponse("handler.commodity.noCargoCapacity");
        }
        if (criteria.getMaxLsFromArrival() == 0) {
            return StringUtls.localizedResponse("handler.commodity.maxDistanceFromArrivalNoSet");
        }

        int toSell = tonnesInHold(commodity);
        int wanted = toSell > 0 ? toSell : criteria.getMaxCargo();

        List<CommoditySearchResult> results = SpanshCommoditySearch.search(
                commodity, starName, distance, criteria, returnClosest, wanted, TradeSide.SELL);
        if (results.isEmpty()) {
            return StringUtls.localizedResponse("handler.commodity.noBuyer");
        }
        List<CommoditySearchResult> elsewhere = awayFromThisPad(results);
        if (elsewhere.isEmpty()) {
            return StringUtls.localizedResponse("handler.commodity.onlyMarketIsHere");
        }
        CommoditySearchResult result = elsewhere.getFirst();

        String reminder = StringUtls.localizedResponse(
                result.isFleetCarrier() ? "handler.commodity.sellAtCarrier" : "handler.commodity.sellAt",
                result.getStarSystem(), result.getStationName(), result.getStationType(), result.getPrice());
        if (result.getDistanceFromPlayer() > distance) {
            reminder += " " + StringUtls.localizedResponse("handler.commodity.beyondRange",
                    distance, Math.round(result.getDistanceFromPlayer()));
        }
        // Only worth saying when the commander is actually carrying the good: telling someone who asked a
        // planning question that the buyer wants "less than a full hold" answers a question nobody asked.
        if (toSell > 0 && result.getSupply() > 0 && result.getSupply() < toSell) {
            reminder += " " + StringUtls.localizedResponse("handler.commodity.partSale", result.getSupply());
        }
        reminder += againstGalacticAverage(commodity, result.getPrice());
        reminder += howOldThatPriceIs(result);
        CompanionRuntime.narrator().filler(reminder, false);
        ReminderManager.getInstance().setReminder(reminder, result.getStarSystem(), result.getStationName(), null);
        recordForOverlay(result, TradeSide.SELL, toSell);

        return new RoutePlotter().plotRoute(result.getStarSystem());
    }

    /**
     * Whether this result is the port the ship is standing on right now.
     * <p>
     * The best market in range is often the one the commander is already docked at - most of all on a second
     * search, made after flying to the first answer and not liking the price. Telling them to "head to" the
     * pad under the ship is worse than useless: the route plotter declines to plot a route to the system it
     * is already in, so the commander hears a destination, sees no galaxy map, and cannot tell a working
     * search from a broken one.
     * <p>
     * Both halves are checked. {@link DockedMarket} knows the port by name because the location tables
     * cannot answer this - they are keyed by bodyId and {@code Docked} carries none - but station names
     * repeat across the galaxy, so the system has to agree as well before a market 80 ly away is mistaken
     * for the one under the ship.
     */
    static boolean standingOn(CommoditySearchResult result) {
        if (!DockedMarket.getInstance().isOn(result.getStationName())) return false;
        String here = PlayerSession.getInstance().getPrimaryStarName();
        return here != null && here.equalsIgnoreCase(result.getStarSystem());
    }

    /**
     * How the price compares with the galactic average, as a sentence to append - or nothing at all when no
     * market the commander has ever opened has listed the good.
     * <p>
     * <b>Why the average and not a profit.</b> A price alone says nothing: 57,844 a tonne for Tritium is a
     * good sale or a poor one entirely depending on the 51,294 it is measured against. Whether the trade
     * shows a PROFIT would need the cost basis, and the journal never gives one - {@code CargoTransfer},
     * how a carrier owner loads most of what they sell, carries no price at all. So the figure quoted is the
     * one that is actually known, and the commander draws their own conclusion from it.
     * <p>
     * Stated as a plain percentage either way rather than as praise. Above average is a good sale and a dear
     * purchase, so which direction is welcome depends on the side, and the commander knows which side they
     * are on.
     */
    static String againstGalacticAverage(String commodity, double price) {
        OptionalInt average = CommodityMeanPriceManager.getInstance()
                .meanPrice(FuzzySearch.commoditySymbol(commodity));
        if (average.isEmpty() || price <= 0) return "";

        int mean = average.getAsInt();
        long percent = Math.round(Math.abs(price - mean) * 100.0 / mean);
        if (percent < AVERAGE_ENOUGH_PERCENT) {
            return " " + StringUtls.localizedResponse("handler.commodity.priceAtAverage", mean);
        }
        return " " + StringUtls.localizedResponse(
                price > mean ? "handler.commodity.priceAboveAverage" : "handler.commodity.priceBelowAverage",
                percent, mean);
    }

    /**
     * A warning that the quoted price is second-hand and old, or nothing when it is neither.
     * <p>
     * <b>Why this is worth a sentence.</b> Spansh is crowd-sourced: its price is whatever the last commander
     * to fly through uploaded, and it does not go stale gracefully - a market's price moves as its demand is
     * consumed. Measured live: Spansh quoted Bari Gateway at 57,844 a tonne for Tritium from a row ten days
     * old, the commander crossed two systems on the strength of it, and the board there was paying 53,992 -
     * 3.4 million credits short over a full hold. Saying "they pay" of a number like that is a promise the
     * app is in no position to make.
     * <p>
     * Silent when the figure came from a board the commander opened themselves, because then it is not a
     * claim at all.
     */
    private static String howOldThatPriceIs(CommoditySearchResult result) {
        if (result.isSeenFirstHand()) return "";
        OptionalLong age = SpanshCommoditySearch.daysSinceUpdate(result.getMarketUpdatedAt());
        if (age.isEmpty() || age.getAsLong() < STALE_AFTER_DAYS) return "";
        return " " + StringUtls.localizedResponse("handler.commodity.priceAge", age.getAsLong());
    }

    /**
     * The candidates minus the pad the ship is standing on.
     * <p>
     * <b>Why the local market is dropped rather than offered.</b> This command's promise is a course, not a
     * price: it ends by plotting a route, and the plotter cannot plot into the system it is already in. So
     * naming the port under the ship produces an answer with no galaxy map behind it - and the commander has
     * no way to type "Col 285 Sector IB-X d1-60" by hand, least of all in VR. A commander who asks where to
     * sell while docked is asking where ELSE, because selling HERE needs no search at all.
     * <p>
     * Nor is the local price worth announcing: whether a sale here is a loss depends on what the cargo cost,
     * and the journal never says. {@code CargoTransfer} - how a carrier owner loads most of what they sell -
     * carries no price at all, so a "you could sell here" line would be a guess dressed as advice.
     */
    static List<CommoditySearchResult> awayFromThisPad(List<CommoditySearchResult> markets) {
        return markets.stream().filter(market -> !standingOn(market)).toList();
    }

    /**
     * Tonnes of {@code commodity} in the ship's hold right now, or zero when none of it is aboard.
     * <p>
     * The hold speaks journal symbols and the search speaks Spansh's English names, so the two are joined
     * through the commodities table the same way the mission cargo search joins them.
     */
    static int tonnesInHold(String commodity) {
        String symbol = FuzzySearch.commoditySymbol(commodity);
        if (symbol == null) return 0;
        GameEvents.CargoEvent cargo = PlayerSession.getInstance().getShipCargo();
        if (cargo == null || cargo.getInventory() == null) return 0;
        for (GameEvents.Inventory item : cargo.getInventory()) {
            if (item.getName() != null && symbol.equalsIgnoreCase(JournalSymbol.normalize(item.getName()))) {
                return (int) item.getCount();
            }
        }
        return 0;
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
        // Nobody said how many were wanted, so the card shows what is there rather than a made-up figure.
        recordForOverlay(result, TradeSide.BUY, 0);
    }

    /**
     * As above, for either side of the counter.
     *
     * @param units tonnes the commander will actually trade, or 0 when nobody said - the card then shows
     *              what the market has room for instead
     */
    private static void recordForOverlay(CommoditySearchResult result, TradeSide side, int units) {
        FoundMarket found = new FoundMarket();
        found.setCommodity(result.getCommodity());
        found.setStarSystem(result.getStarSystem());
        found.setStationName(result.getStationName());
        found.setStationType(result.getStationType());
        found.setPrice(Math.round(result.getPrice()));
        found.setSupply(result.getSupply());
        found.setFleetCarrier(result.isFleetCarrier());
        found.setSide(side.name());

        FoundLine line = new FoundLine();
        line.setCommodity(result.getCommodity());
        line.setPrice(Math.round(result.getPrice()));
        line.setSupply(result.getSupply());
        line.setUnitsToBuy(Math.min(units, (int) Math.min(result.getSupply(), Integer.MAX_VALUE)));

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
        found.setSide(TradeSide.BUY.name());

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
