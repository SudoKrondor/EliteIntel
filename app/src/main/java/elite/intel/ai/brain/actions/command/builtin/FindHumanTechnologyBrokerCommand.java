package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
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

    private static final int DEFAULT_RANGE = 250;

    @Override
    public String id() {
        return CommandIds.FIND_HUMAN_TECHNOLOGY_BROKER;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Number range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE);
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.broker.searching", BrokerType.HUMAN.getType())));
        TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
        RoutePlotter routePlotter = new RoutePlotter();
        String location = search.location(null, BrokerType.HUMAN, range);
        if (location == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.broker.noHuman")));
        } else {
            routePlotter.plotRoute(location);
        }
    }
}
