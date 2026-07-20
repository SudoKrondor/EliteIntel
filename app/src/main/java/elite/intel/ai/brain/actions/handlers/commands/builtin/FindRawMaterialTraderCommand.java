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
 * Self-describing "find raw material trader" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindRawMaterialTraderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindRawMaterialTraderCommand implements IntelCommand {
    public static final String ID = "find_raw_material_trader";

    @Override
    public String llmDescription() {
        return "Find and plot a route to the nearest Raw material trader.";
    }


    private static final int DEFAULT_RANGE = 250;

    @Override
    public String id() {
        return ID;
    }

    /// route plotting is accessible anywhere in the game
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Number range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE);
        CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.trader.searching", TraderType.RAW.getType()), false);
        TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
        RoutePlotter routePlotter = new RoutePlotter();
        return routePlotter.plotRoute(search.location(TraderType.RAW, null, range.intValue()));
    }
}
