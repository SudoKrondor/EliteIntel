package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.commodity.CommoditySearchResult;
import elite.intel.gameapi.search.spansh.commodity.SpanshCommoditySearch;
import elite.intel.gameapi.search.spansh.traderoute.TradeRouteSearchCriteria;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.List;


/**
 * Self-describing "find commodity" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindCommodityHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindCommodityCommand implements IntelCommand {
    public static final String ID = "find_commodity";

    @Override
    public String llmDescription() {
        // The BUY-only sentence earns its place: the reducer scores topic, not direction, so a sell question
        // ("where can I sell my gold", "ou vendre l'or") offers this tool at the top of the band. Nothing we
        // have answers a sell search, and answering it with a buy search sends the commander to a market
        // that wants payment for what he came to unload.
        return "Find where to buy the commodity in 'key' within 'max_distance' ly and plot a route to it; "
                + "'state' true = nearest market, false = best-price market. "
                + "BUYING ONLY: this cannot find where to SELL cargo. If the commander asks where to sell, "
                + "say so instead of calling this.";
    }


    /**
     * Human space is about this wide around Sol; the fallback radius when the ship's jump range is unknown.
     */
    private static final int INHABITED_BUBBLE_LY = 1000;

    private static final String PARAM_KEY = "key";
    private static final String PARAM_MAX_DISTANCE = "max_distance";
    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final TradeProfileManager tradeProfileManager = TradeProfileManager.getInstance();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
                "The commodity (market good) to search for, e.g. gold, tritium, painite.",
                List.of("gold", "tritium"),
                "Extract the commodity name verbatim in lower case; do not translate.");
        key.validate();
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                PARAM_MAX_DISTANCE, "number", false,
                "Maximum galactic search radius in light years (ly). If omitted, a default range is used.",
                List.of("80", "150"),
                "Extract the distance limit in light years if the commander states one, ALWAYS as digits: "
                        + "the 80 in 'find gold within 80 ly', and 200 for 'within two hundred light years'.");
        maxDistance.validate();
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE, "boolean", false,
                "Search mode: true = nearest market (by distance); false = best price / where to buy.",
                List.of("true", "false"),
                "Set true when the commander says 'nearest' or 'closest'; otherwise false.");
        state.validate();
        return List.of(key, maxDistance, state);
    }

    @Override
    public String id() {
        return ID;
    }

    /// Route plotting available anywhere in the game
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
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
    private int defaultRange() {
        int shipRange = (int) playerSession.getShipLoadout().getMaxJumpRange() * 2;
        return shipRange < 1 ? INHABITED_BUBBLE_LY : shipRange;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement key = params.get(PARAM_KEY);
        JsonElement stateEl = params.get(PARAM_STATE);
        boolean returnClosest = stateEl != null && stateEl.getAsBoolean();
        // Through the shared reader like every other "find X within Y light years" command, so a radius
        // spoken in words ("two hundred") is honoured here too instead of silently becoming the default.
        int distance = GetNumberFromParam.extractRangeParameter(params, defaultRange()).intValue();
        String starName = playerSession.getPrimaryStarName();

        if (key == null) {
            return StringUtls.localizedResponse("handler.commodity.specify");
        }

        // Our own table's spelling, passed on untouched: Spansh matches a commodity name exactly, and
        // title-casing it here quietly broke 23 goods - "Agri-Medicines" became "Agri-medicines",
        // "H.E. Suits" became "H.e. Suits", and the market search found nothing anywhere in the galaxy.
        String commodity = FuzzySearch.fuzzyCommodityMatch(key.getAsString(), 3);

        if (commodity == null) {
            return StringUtls.localizedResponse("handler.commodity.notFound", key.getAsString());
        }

        String searchMode = StringUtls.localizedResponse(returnClosest ? "handler.commodity.modeNearest" : "handler.commodity.modeBest");
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.commodity.searching", searchMode, commodity, distance), false);
        TradeRouteSearchCriteria tradeProfileManagerCriteria = tradeProfileManager.getCriteria(false);
        // Null until the game has told us which ship we are in, and a ship is what carries the hold the
        // search sizes itself to.
        if (tradeProfileManagerCriteria == null) {
            return StringUtls.localizedResponse("handler.commodity.noCargoCapacity");
        }
        int cargoCapacity = tradeProfileManagerCriteria.getMaxCargo();
        if (cargoCapacity == 0) {
            return StringUtls.localizedResponse("handler.commodity.noCargoCapacity");
        }
        int maxDistanceFromArrival = tradeProfileManagerCriteria.getMaxLsFromArrival();
        if (maxDistanceFromArrival == 0) {
            return StringUtls.localizedResponse("handler.commodity.maxDistanceFromArrivalNoSet");
        }
        List<CommoditySearchResult> results = SpanshCommoditySearch.search(
                commodity,
                starName,
                distance,
                tradeProfileManagerCriteria,
                returnClosest
        );
        if (results.isEmpty()) {
            return StringUtls.localizedResponse("handler.commodity.noMatch");
        }
        ReminderManager reminderManager = ReminderManager.getInstance();
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
        // it says how much is there - otherwise the shortfall is discovered at the commodity market.
        if (result.getSupply() > 0 && result.getSupply() < cargoCapacity) {
            reminder += " " + StringUtls.localizedResponse("handler.commodity.partLoad", result.getSupply());
        }
        CompanionRuntime.narrator().filler(reminder, false);
        reminderManager.setReminder(reminder, result.getStarSystem(), result.getStationName(), null);

        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(result.getStarSystem());
        return null;
    }
}
