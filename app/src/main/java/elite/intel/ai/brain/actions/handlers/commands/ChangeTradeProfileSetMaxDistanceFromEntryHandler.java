package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

public class ChangeTradeProfileSetMaxDistanceFromEntryHandler implements CommandHandler{

    @Override public void handle(String action, JsonObject params, String responseText) {
        Integer distanceFromEntry = StringUtls.getIntSafely(params.get("key").getAsString());

        if(distanceFromEntry == null){
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.invalidDistance")));
            return;
        }

        TradeProfileManager manager = TradeProfileManager.getInstance();
        if(manager.setDistanceFromSystemEntry(distanceFromEntry)) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.distanceFromEntry", distanceFromEntry)));
        }
    }
}
