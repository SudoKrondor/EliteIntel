package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.station.TradersAndBrokersSearch;
import elite.intel.search.spansh.station.traderandbroker.BrokerType;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

/**
 * Self-describing "find human technology broker" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindHumanTechnologyBrokerHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindHumanTechnologyBrokerCommand implements IntelCommand {
    public static final String ID = "find_human_technology_broker";

    @Override public String llmDescription() { return "Find the nearest human technology broker."; }


    private static final int DEFAULT_RANGE = 250;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        Number range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE);
        TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
        RoutePlotter routePlotter = new RoutePlotter();
        String location = search.location(null, BrokerType.HUMAN, range);
        if (location == null) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.broker.noHuman"));
        }
        JsonObject plotOutcome = routePlotter.plotRoute(location);
        if (plotOutcome != null) {
            return plotOutcome;
        }
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.broker.searching", BrokerType.HUMAN.getType()));
    }
}
