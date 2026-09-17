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
 * Self-describing "find commodity" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindCommodityHandler,
 * routed through CommandRegistry via the self-describing model.
 * <p>
 * Its job ends at turning what the commander said into a name the commodities table knows;
 * {@link CommodityTradeSearch} does the searching, and {@link FindMissionCommodityCommand} reaches
 * the same place from the mission board instead of from a spoken name.
 */
@RegisterCommand
public final class FindCommodityCommand implements IntelCommand {
    public static final String ID = "find_commodity";

    @Override
    public String llmDescription() {
        // The BUY-only sentence earns its place: the reducer scores topic, not direction, so a sell question
        // ("where can I sell my gold", "ou vendre l'or") offers this tool at the top of the band too, and
        // answering it with a buy search sends the commander to a market that wants payment for what he
        // came to unload. Naming the sibling is what turns "do not do this" into an answer.
        return "Find where to BUY the commodity in 'key' within 'max_distance' ly and plot a route to it; "
                + "'state' true = nearest market, false = best-price market. "
                + "BUYING ONLY: for where to SELL cargo, call find_where_to_sell_commodity instead.";
    }


    private static final String PARAM_KEY = "key";
    private static final String PARAM_MAX_DISTANCE = "max_distance";
    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

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
        // title-casing it here quietly broke 23 goods - "Agri-Medicines" became "Agri-medicines",
        // "H.E. Suits" became "H.e. Suits", and the market search found nothing anywhere in the galaxy.
        String commodity = FuzzySearch.fuzzyCommodityMatch(key.getAsString(), 3);

        if (commodity == null) {
            return StringUtls.localizedResponse("handler.commodity.notFound", key.getAsString());
        }
        // A good learned from a non-English client has no English name yet, and Spansh matches nothing else.
        if (!FuzzySearch.hasTradeName(commodity)) {
            return StringUtls.localizedResponse("handler.commodity.tradeNameUnknown", FuzzySearch.localizedCommodityName(commodity));
        }
        return CommodityTradeSearch.findAndPlot(commodity, distance, returnClosest);
    }
}
