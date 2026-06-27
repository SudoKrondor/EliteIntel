package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.search.spansh.station.TradersAndBrokersSearch;
import elite.intel.search.spansh.station.traderandbroker.BrokerType;
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

    @Override public String llmDescription() { return "Find the nearest guardian technology broker."; }


    private static final int DEFAULT_RANGE = 250;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Number range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE);
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.broker.searching", BrokerType.GUARDIAN.getType())));
        TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
        RoutePlotter routePlotter = new RoutePlotter();

        String location = search.location(null, BrokerType.GUARDIAN, range);
        if (location == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.broker.noGuardian")));
        } else {
            routePlotter.plotRoute(location);
        }
    }
}
