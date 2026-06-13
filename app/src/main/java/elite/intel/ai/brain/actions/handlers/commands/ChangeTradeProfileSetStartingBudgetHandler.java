package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

public class ChangeTradeProfileSetStartingBudgetHandler implements CommandHandler {

    @Override public void handle(String action, JsonObject params, String responseText) {
        Integer budget = StringUtls.getIntSafely(params.get("key").getAsString());
        if (budget == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.invalidBudget")));
           return;
        }

        TradeProfileManager manager = TradeProfileManager.getInstance();
        if(manager.setStartingCapitol(budget)) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.startingBudget", budget)));
        }
    }
}
