package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.FuzzySearch;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.List;

/**
 * "Where can I sell this?" - the mirror of {@link FindCommodityCommand}, and its own tool rather than a
 * flag on that one.
 * <p>
 * <b>Why a separate command.</b> The reducer scores topic and blurs polarity: "where can I sell tritium"
 * and "where can I buy tritium" embed almost identically, so both tools are offered for either question
 * and the model reads the descriptions to choose. A single tool with a direction parameter would face the
 * same model with the same ambiguity and no second description to distinguish it - and a wrong boolean is
 * silent, where a wrong tool at least names itself in the log. Both descriptions therefore say their
 * direction in capitals and name the other tool.
 * <p>
 * Its job ends at turning what the commander said into a name the commodities table knows;
 * {@link CommodityTradeSearch#findSaleAndPlot} does the searching, on the same escalation ladder, reminder
 * and route as the buy search.
 */
@RegisterCommand
public final class SellCommodityCommand implements IntelCommand {
    public static final String ID = "find_where_to_sell_commodity";

    @Override
    public String llmDescription() {
        return "Find where to SELL the commodity in 'key' - a market with demand for it, within "
                + "'max_distance' ly - and plot a route to it; 'state' true = nearest buyer, false = "
                + "best-paying buyer. SELLING ONLY: for where to BUY a commodity, call find_commodity instead.";
    }

    private static final String PARAM_KEY = "key";
    private static final String PARAM_MAX_DISTANCE = "max_distance";
    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
                "The commodity (market good) to sell, e.g. gold, tritium, painite.",
                List.of("tritium", "gold"),
                "Extract the commodity name verbatim in lower case; do not translate.");
        key.validate();
        ActionParameterSpec maxDistance = new ActionParameterSpec(
                PARAM_MAX_DISTANCE, "number", false,
                "Maximum galactic search radius in light years (ly). If omitted, a default range is used.",
                List.of("80", "150"),
                "Extract the distance limit in light years if the commander states one, ALWAYS as digits: "
                        + "the 80 in 'where can I sell gold within 80 ly', and 200 for 'within two hundred light years'.");
        maxDistance.validate();
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE, "boolean", false,
                "Search mode: true = nearest buyer (by distance); false = best price paid.",
                List.of("true", "false"),
                "Set true when the commander says 'nearest' or 'closest'; otherwise false.");
        state.validate();
        return List.of(key, maxDistance, state);
    }

    @Override
    public String id() {
        return ID;
    }

    /// Route plotting available anywhere in the game; asking where a good sells is a planning question
    /// the commander may ask with an empty hold.
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement key = params.get(PARAM_KEY);
        JsonElement stateEl = params.get(PARAM_STATE);
        boolean returnClosest = stateEl != null && stateEl.getAsBoolean();
        // Through the shared reader like every other "find X within Y light years" command, so a radius
        // spoken in words ("two hundred") is honoured here too instead of silently becoming the default.
        int distance = GetNumberFromParam.extractRangeParameter(params, CommodityTradeSearch.defaultRange()).intValue();

        if (key == null) {
            return StringUtls.localizedResponse("handler.commodity.specify");
        }

        // Our own table's spelling, passed on untouched: Spansh matches a commodity name exactly, and
        // title-casing it here quietly broke 23 goods - see FindCommodityCommand.
        String commodity = FuzzySearch.fuzzyCommodityMatch(key.getAsString(), 3);

        if (commodity == null) {
            return StringUtls.localizedResponse("handler.commodity.notFound", key.getAsString());
        }
        return CommodityTradeSearch.findSaleAndPlot(commodity, distance, returnClosest);
    }
}
