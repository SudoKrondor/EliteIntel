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
 * Owns its own execution: body migrated 1:1 from the legacy ChangeTradeProfileAllowPermitSystemsHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TradeProfileTogglePermitSystemsCommand implements IntelCommand {
    public static final String ID = "trade_profile_toggle_permit_systems";

    @Override public String llmDescription() { return "Toggle whether trade routes may include permit-locked systems."; }


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec state = new ActionParameterSpec(
                "state",
                "boolean",
                true,
                "Whether permit-locked systems are allowed in route calculations. true = allow, false = disallow.",
                List.of("true", "false"),
                "Determine whether the commander wants to allow or disallow permit-locked systems."
        );
        state.validate();
        return List.of(state);
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
        boolean isOn = params.get("state").getAsBoolean();
        TradeProfileManager profileManager = TradeProfileManager.getInstance();
        if (profileManager.setAllowPermit(isOn)) {
            String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.tradeProfile.permitSystems", state));
        }
        return null;
    }
}
