package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy CancelTradeRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class CancelTradeRouteCommand implements IntelCommand {

    private final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();

    @Override
    public String id() {
        return CommandIds.CANCEL_TRADE_ROUTE;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        tradeRouteManager.clear();
        EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeRoute.cancelled")));
    }
}
