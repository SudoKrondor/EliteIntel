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
 * Owns its own execution: body migrated 1:1 from the legacy ChangeTradeProfileSetMaxDistanceFromEntryHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TradeProfileSetMaxDistanceCommand implements IntelCommand {
    public static final String ID = "trade_profile_set_max_distance";

    @Override public String llmDescription() { return "Set the trade-route maximum distance."; }


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                "key",
                "number",
                true,
                "Maximum distance from the entry/start system arrival star allowed for the trade route, in light seconds (Ls).",
                List.of("50", "100"),
                "Extract the maximum distance the commander wants to allow."
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
        Integer distanceFromEntry = StringUtls.getIntSafely(params.get("key").getAsString());

        if (distanceFromEntry == null) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeProfile.invalidDistance"));
        }

        TradeProfileManager manager = TradeProfileManager.getInstance();
        if (manager.setDistanceFromSystemEntry(distanceFromEntry)) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeProfile.distanceFromEntry", distanceFromEntry));
        }
        return null;
    }
}
