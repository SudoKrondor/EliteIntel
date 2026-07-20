package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ChangeTradeProfileSetMaxStopsHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TradeProfileSetMaxStopsCommand implements IntelCommand {
    public static final String ID = "trade_profile_set_max_stops";

    @Override
    public String llmDescription() {
        return "Set the trade-route profile's maximum number of stops to the value in 'key'.";
    }


    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY,
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

    /** App-side trade-profile setting (no game input); executable in any location. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Integer numberOfStops = StringUtls.getIntSafely(params.get(PARAM_KEY).getAsString());

        if (numberOfStops == null) {
            return StringUtls.localizedLlm("handler.tradeProfile.invalidStops");
        }

        TradeProfileManager profileManager = TradeProfileManager.getInstance();
        if(profileManager.setMaximumStops(numberOfStops)) {
            return StringUtls.localizedLlm("handler.tradeProfile.maxStops", numberOfStops);
        }
        return null;
    }
}
