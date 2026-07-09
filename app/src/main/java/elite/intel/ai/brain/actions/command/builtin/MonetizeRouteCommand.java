package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.companion.CompanionRuntime;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.managers.MonetizeRouteManager;
import elite.intel.db.managers.ReminderManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.search.edsm.monetize.MonetizeRoute;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy MonetizeRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class MonetizeRouteCommand implements IntelCommand {
    public static final String ID = "monetize_route";

    @Override
    public String llmDescription() {
        return "Find a profitable buy/sell commodity pair along the currently plotted route and save it as a reminder, to earn credits while travelling.";
    }


    private final MonetizeRouteManager monetizeRouteManager = MonetizeRouteManager.getInstance();
    private final ReminderManager reminderManager = ReminderManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** App-side trade optimization (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        ShipManager shipManager = ShipManager.getInstance();
        if (shipManager.getShip() == null || shipManager.getShip().getCargoCapacity() < 1) {
            return StringUtls.localizedLlm("handler.tradeRoute.shipNoCapacity");
        }
        CompanionRuntime.narrator().filler(StringUtls.localizedLlm("handler.tradeRoute.searchingMarkets"), false);

        MonetizeRoute.TradeTransaction tradeTuple = monetizeRouteManager.monetizeRoute();

        if (tradeTuple == null) {
            return StringUtls.localizedLlm("handler.tradeRoute.noTradeFound");
        } else {
            String reminder = StringUtls.localizedLlm("handler.tradeRoute.tradeReminder",
                    tradeTuple.getSource().getStarSystem(),
                    tradeTuple.getSource().getStationName(),
                    tradeTuple.getSource().getCommodity(),
                    tradeTuple.getDestination().getStarSystem(),
                    tradeTuple.getDestination().getStationName());

            reminderManager.setReminder(reminder, tradeTuple.getSource().getStarSystem());

            return StringUtls.localizedLlm("handler.tradeRoute.tradeFound", reminder);
        }
    }
}
