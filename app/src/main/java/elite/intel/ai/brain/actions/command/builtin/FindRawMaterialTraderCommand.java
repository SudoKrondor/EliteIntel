package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.station.TradersAndBrokersSearch;
import elite.intel.search.spansh.station.traderandbroker.TraderType;
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

    @Override public String llmDescription() { return "Find the nearest raw material trader."; }


    private static final int DEFAULT_RANGE = 250;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (!status.isInSrv() && !status.isInMainShip() && !status.isOnFoot()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.navigate.notInShipSrvOrFoot"));
        }
        Number range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE);
        TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
        RoutePlotter routePlotter = new RoutePlotter();
        JsonObject plotOutcome = routePlotter.plotRoute(search.location(TraderType.RAW, null, range.intValue()));
        if (plotOutcome != null) {
            return plotOutcome;
        }
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.trader.searching", TraderType.RAW.getType()));
    }
}
