package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.companion.CompanionRuntime;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.station.TradersAndBrokersSearch;
import elite.intel.search.spansh.station.traderandbroker.BrokerType;
import elite.intel.session.Status;
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

    @Override
    public String llmDescription() {
        return "Find and plot a route to the nearest Human technology broker (unlocks tech-broker modules).";
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
        CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.broker.searching", BrokerType.HUMAN.getType()), false);
        TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
        RoutePlotter routePlotter = new RoutePlotter();
        String location = search.location(null, BrokerType.HUMAN, range);
        if (location == null) {
            return StringUtls.localizedLlm("handler.broker.noHuman");
        }

        return routePlotter.plotRoute(location);
    }
}
