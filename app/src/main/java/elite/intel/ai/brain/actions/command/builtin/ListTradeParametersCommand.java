package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.util.StringUtls;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ListAvailableTradeRouteProfilesHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ListTradeParametersCommand implements IntelCommand {
    public static final String ID = "list_trade_parameters";

    @Override public String llmDescription() { return "List the current trade-route search parameters."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeRoute.listParams"));
    }
}
