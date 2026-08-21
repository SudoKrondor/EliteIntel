package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.commodity.CommoditySearchResult;
import elite.intel.gameapi.search.spansh.commodity.SpanshCommoditySearch;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

import java.util.List;

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
     * Finds the market to buy a hold's worth of {@code commodity} at and plots a route to it.
     */
    static String findAndPlot(String commodity, int distance, boolean returnClosest) {
        return findAndPlot(commodity, distance, returnClosest, SpanshCommoditySearch.WANT_FULL_HOLD);
    }

    /**
     * Finds the market to buy {@code commodity} at and plots a route to it.
     *
     * @param commodity     the commodity in the commodities table's own spelling; Spansh matches it exactly,
     *                      so title-casing it first quietly breaks the 23 goods with punctuation in them
     * @param distance      search radius in light years
     * @param returnClosest true for the nearest market, false for the best price
     * @param wantedUnits   units actually needed, or {@link SpanshCommoditySearch#WANT_FULL_HOLD} for a hold's
     *                      worth. A mission still owing 20 tonnes wants 20, not 300 - both for which markets
     *                      qualify and for whether the answer counts as a part load
     * @return a localized message when the search could not be run or found nothing, or null when it
     * succeeded and the answer has already been spoken
     */
    static String findAndPlot(String commodity, int distance, boolean returnClosest, int wantedUnits) {
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
        // What this trip is actually for: the mission's remainder, or the whole hold when nobody said.
        // Never more than the ship can carry - a mission wanting 200 tonnes into a 100 tonne hold is two
        // trips, and asking Spansh for markets holding 200 would pass over the ones that can fill this one.
        int wanted = wantedUnits <= SpanshCommoditySearch.WANT_FULL_HOLD
                ? cargoCapacity
                : Math.min(wantedUnits, cargoCapacity);
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
            reminder += " " + StringUtls.localizedResponse(
                    wanted == cargoCapacity ? "handler.commodity.partLoad" : "handler.commodity.partLoadShort",
                    result.getSupply(), wanted);
        }
        CompanionRuntime.narrator().filler(reminder, false);
        ReminderManager.getInstance().setReminder(reminder, result.getStarSystem(), result.getStationName(), null);

        new RoutePlotter().plotRoute(result.getStarSystem());
        return null;
    }
}
