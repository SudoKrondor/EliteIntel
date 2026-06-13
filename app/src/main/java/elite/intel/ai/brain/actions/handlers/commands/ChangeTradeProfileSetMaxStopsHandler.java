package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

public class ChangeTradeProfileSetMaxStopsHandler implements CommandHandler {
    @Override public void handle(String action, JsonObject params, String responseText) {
        Integer numberOfStops = StringUtls.getIntSafely(params.get("key").getAsString());

        if (numberOfStops == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.invalidStops")));
            return;
        }

        TradeProfileManager profileManager = TradeProfileManager.getInstance();
        if(profileManager.setMaximumStops(numberOfStops)) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.maxStops", numberOfStops)));
        }
    }
}
