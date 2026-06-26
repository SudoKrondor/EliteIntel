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
 * Self-describing "find manufactured material trader" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindManufacturedMaterialTraderHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindManufacturedMaterialTraderCommand implements IntelCommand {
    public static final String ID = "find_manufactured_material_trader";

    @Override public String llmDescription() { return "Find the nearest manufactured material trader."; }


    private static final int DEFAULT_RANGE = 250;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if (!status.isInSrv() && !status.isInMainShip()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.navigate.notInShipOrSrv"));
        }
        Number range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE);
        TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
        RoutePlotter routePlotter = new RoutePlotter();
        JsonObject plotOutcome = routePlotter.plotRoute(search.location(TraderType.MANUFACTURED, null, range));
        if (plotOutcome != null) {
            return plotOutcome;
        }
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.trader.searching", TraderType.MANUFACTURED.getType()));
    }
}
