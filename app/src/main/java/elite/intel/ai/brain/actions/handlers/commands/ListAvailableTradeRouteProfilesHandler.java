package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

public class ListAvailableTradeRouteProfilesHandler implements CommandHandler {

    @Override public void handle(String action, JsonObject params, String responseText) {
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.listParams")));
    }
}
