package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.search.edsm.monetize.MonetizeRoute;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy MonetizeRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class MonetizeRouteCommand implements IntelCommand {
    public static final String ID = "monetize_route";


    private final MonetizeRouteManager monetizeRouteManager = MonetizeRouteManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        ShipManager shipManager = ShipManager.getInstance();
        if (shipManager.getShip() == null || shipManager.getShip().getCargoCapacity() < 1) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.shipNoCapacity")));
            return;
        }
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.searchingMarkets")));

        MonetizeRoute.TradeTransaction tradeTuple = monetizeRouteManager.monetizeRoute();

        if (tradeTuple == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.noTradeFound")));
        } else {
            String reminder = StringUtls.localizedLlm("handler.tradeRoute.tradeReminder",
                    tradeTuple.getSource().getStarSystem(),
                    tradeTuple.getSource().getStationName(),
                    tradeTuple.getSource().getCommodity(),
                    tradeTuple.getDestination().getStarSystem(),
                    tradeTuple.getDestination().getStationName());

            reminderManager.setReminder(reminder, tradeTuple.getSource().getStarSystem());

            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.tradeFound", reminder)));
        }
    }
}
