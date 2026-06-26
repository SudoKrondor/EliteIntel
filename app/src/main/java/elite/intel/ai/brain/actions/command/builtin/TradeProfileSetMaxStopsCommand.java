package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ChangeTradeProfileSetMaxStopsHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TradeProfileSetMaxStopsCommand implements IntelCommand {
    public static final String ID = "trade_profile_set_max_stops";

    @Override public String llmDescription() { return "Set the trade-route maximum number of stops."; }


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key",
                "number",
                true,
                "Maximum number of stops (hops) allowed in the trade route.",
                List.of("3", "5"),
                "Extract the number of stops the commander wants to allow."
        );
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        Integer numberOfStops = StringUtls.getIntSafely(params.get("key").getAsString());

        if (numberOfStops == null) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeProfile.invalidStops"));
        }

        TradeProfileManager profileManager = TradeProfileManager.getInstance();
        if (profileManager.setMaximumStops(numberOfStops)) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeProfile.maxStops", numberOfStops));
        }
        return null;
    }
}
