package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.spansh.station.TradersAndBrokersSearch;
import elite.intel.search.spansh.station.traderandbroker.BrokerType;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

public class FindHumanTechnologyBrokerHandler implements CommandHandler {

    public static final int DEFAULT_RANGE = 250;


    @Override public void handle(String action, JsonObject params, String responseText) {
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
