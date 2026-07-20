package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.station.TradersAndBrokersSearch;
import elite.intel.gameapi.search.spansh.station.traderandbroker.TraderType;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

/**
 * Self-describing "find encoded material trader" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindEncodedMaterialTraderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindEncodedMaterialTraderCommand implements IntelCommand {
    public static final String ID = "find_encoded_material_trader";

    @Override
    public String llmDescription() {
        return "Find and plot a route to the nearest Encoded material trader (trades encoded/data engineering materials).";
    }


    private static final int DEFAULT_RANGE = 250;

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
    public String execute(JsonObject params, String responseText) {
        Number range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE);
        CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.trader.searching", TraderType.ENCODED.getType()), false);
        TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
        RoutePlotter routePlotter = new RoutePlotter();
        return routePlotter.plotRoute(search.location(TraderType.ENCODED, null, range));
    }
}
