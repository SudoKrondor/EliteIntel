package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.gameapi.search.spansh.station.TradersAndBrokersSearch;
import elite.intel.gameapi.search.spansh.station.traderandbroker.BrokerType;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

/**
 * Self-describing "find guardian technology broker" command.
 * Owns its own execution: body migrated 1:1 from the legacy FindGuadrianTechnologyBroker,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class FindGuardianTechnologyBrokerCommand implements IntelCommand {
    public static final String ID = "find_guardian_technology_broker";

    @Override
    public String llmDescription() {
        return "Find and plot a route to the nearest Guardian technology broker (unlocks Guardian modules and weapons).";
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
        CompanionRuntime.narrator().filler(StringUtls.localizedResponse("handler.broker.searching", BrokerType.GUARDIAN.getType()), false);
        TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
        RoutePlotter routePlotter = new RoutePlotter();

        String location = search.location(null, BrokerType.GUARDIAN, range);
        if (location == null) {
            return StringUtls.localizedResponse("handler.broker.noGuardian");
        }

        return routePlotter.plotRoute(location);
    }
}
