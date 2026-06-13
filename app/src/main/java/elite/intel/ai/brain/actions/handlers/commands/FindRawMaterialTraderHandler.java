package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.spansh.station.TradersAndBrokersSearch;
import elite.intel.search.spansh.station.traderandbroker.TraderType;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GetNumberFromParam;

public class FindRawMaterialTraderHandler implements CommandHandler {

    public static final int DEFAULT_RANGE = 250;


    @Override public void handle(String action, JsonObject params, String responseText) {
        Status status = Status.getInstance();
        if(status.isInSrv() || status.isInMainShip() || status.isOnFoot()) {
            Number range = GetNumberFromParam.extractRangeParameter(params, DEFAULT_RANGE);
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.trader.searching", TraderType.RAW.getType())));
            TradersAndBrokersSearch search = TradersAndBrokersSearch.getInstance();
            RoutePlotter routePlotter = new RoutePlotter();
            routePlotter.plotRoute(search.location(TraderType.RAW, null, range.intValue()));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.navigate.notInShipSrvOrFoot")));
        }
    }
}
