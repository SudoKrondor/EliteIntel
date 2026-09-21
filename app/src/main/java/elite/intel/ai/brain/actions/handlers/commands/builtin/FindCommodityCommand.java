package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.FuzzySearch;
import elite.intel.gameapi.search.spansh.station.outfitting.WantedModule;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

import java.util.List;


/**
 * Self-describing "find commodity" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindCommodityHandler,
 * routed through CommandRegistry via the self-describing model.
 * <p>
 * Its job ends at turning what the commander said into a name one of the catalogues knows: a commodity,
 * and {@link CommodityTradeSearch} does the searching; or a ship module, and {@link ShipModuleSearch} does.
 * {@link FindMissionCommodityCommand} reaches the commodity search from the mission board instead of
 * from a spoken name.
 * <p>
 * WHY one command for both: "find where I can buy X" is the same sentence whether X is gold or a fuel
 * scoop, and no model - least of all a small local one - can be relied on to know which catalogue holds
 * X. The catalogues can: what was said is matched against both, and the one that recognises it decides.
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
        return "Find where to BUY the commodity or ship module in 'key' within 'max_distance' ly and plot a route "
                + "to it; 'state' true = nearest market, false = best-price market. "
                + "BUYING ONLY: for where to SELL cargo, call find_where_to_sell_commodity instead.";
    }


    private static final String PARAM_KEY = "key";
    private static final String PARAM_MAX_DISTANCE = "max_distance";
    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY, "string", true,
                "The commodity (market good) or ship module to search for, e.g. gold, tritium, painite, "
                        + "fuel scoop size 6 class b.",
                List.of("gold", "tritium", "fuel scoop size 6 class b"),
                "Extract the commodity or module name verbatim in lower case; do not translate. "
                        + "For a module KEEP any size, class and mount the commander said, inside 'key'.");
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

        SpokenModule module = SpokenModule.parse(key.getAsString());
        // A size, class or mount settles it: no commodity has one. Otherwise a word-for-word module name
        // wins before the commodity fuzzy match gets a chance to bend it into a good.
        if (module.isSpecific() || FuzzySearch.isShipModuleName(module.name())) {
            return findModule(module, distance, returnClosest);
        }

        // Our own table's spelling, passed on untouched: Spansh matches a commodity name exactly, and
        // title-casing it here quietly broke 23 goods - "Agri-Medicines" became "Agri-medicines",
        // "H.E. Suits" became "H.e. Suits", and the market search found nothing anywhere in the galaxy.
        String commodity = FuzzySearch.fuzzyCommodityMatch(key.getAsString(), 3);

        if (commodity == null) {
            return findModule(module, distance, returnClosest);
        }
        // A good learned from a non-English client has no English name yet, and Spansh matches nothing else.
        if (!FuzzySearch.hasTradeName(commodity)) {
            return StringUtls.localizedResponse("handler.commodity.tradeNameUnknown", FuzzySearch.localizedCommodityName(commodity));
        }
        return CommodityTradeSearch.findAndPlot(commodity, distance, returnClosest);
    }

    /**
     * The module branch: the spoken name resolved against the module catalogue, then searched with whatever
     * designation the commander put around it.
     */
    private static String findModule(SpokenModule module, int distance, boolean returnClosest) {
        String name = FuzzySearch.fuzzyShipModuleMatch(module.name(), 3);
        if (name == null) {
            return StringUtls.localizedResponse("handler.module.notFound", module.name());
        }
        WantedModule wanted = new WantedModule(FuzzySearch.shipModuleSpellings(name), module.size(), module.rating(), module.mount());
        return ShipModuleSearch.findAndPlot(wanted, distance, returnClosest);
    }
}
