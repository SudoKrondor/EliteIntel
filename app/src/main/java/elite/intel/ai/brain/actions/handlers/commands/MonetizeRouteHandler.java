package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.search.edsm.monetize.MonetizeRoute;
import elite.intel.util.StringUtls;

public class MonetizeRouteHandler implements CommandHandler {

    private final MonetizeRouteManager monetizeRouteManager = MonetizeRouteManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();

    @Override public void handle(String action, JsonObject params, String responseText) {
        ShipManager shipManager = ShipManager.getInstance();
        if (shipManager.getShip() == null || shipManager.getShip().getCargoCapacity() < 1) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.shipNoCapacity")));
            return;
        }
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.searchingMarkets")));

        MonetizeRoute.TradeTransaction tradeTuple = monetizeRouteManager.monetizeRoute();

        if (tradeTuple == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.noTradeFound")));
        } else {
            String reminder = StringUtls.localizedLlm("handler.tradeRoute.tradeReminder",
                    tradeTuple.getSource().getStarSystem(),
                    tradeTuple.getSource().getStationName(),
                    tradeTuple.getSource().getCommodity(),
                    tradeTuple.getDestination().getStarSystem(),
                    tradeTuple.getDestination().getStationName());

            reminderManager.setReminder(reminder, tradeTuple.getSource().getStarSystem());

            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.tradeFound", reminder)));
        }
    }
}
