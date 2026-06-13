package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

public class CancelTradeRouteHandler implements CommandHandler {

    private final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        tradeRouteManager.clear();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.cancelled")));
    }
}