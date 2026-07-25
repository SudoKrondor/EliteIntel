package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.TradeRouteManager;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy CancelTradeRouteHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class CancelTradeRouteCommand implements IntelCommand {
    public static final String ID = "cancel_trade_route";

    @Override
    public String llmDescription() {
        return "Clear and abort the currently planned commodity trade route.";
    }


    private final TradeRouteManager tradeRouteManager = TradeRouteManager.getInstance();

    @Override
    public String id() {
        return ID;
    }

    /** App-side bookkeeping (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        tradeRouteManager.clear();
        return StringUtls.localizedResponse("handler.tradeRoute.cancelled");
    }
}
