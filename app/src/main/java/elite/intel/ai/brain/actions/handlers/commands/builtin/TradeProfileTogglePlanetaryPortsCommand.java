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
 * Owns its own execution: body migrated 1:1 from the legacy ChangeTradeProfileSetIncluidePlanetaryPortsHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TradeProfileTogglePlanetaryPortsCommand implements IntelCommand {
    public static final String ID = "trade_profile_toggle_planetary_ports";

    @Override
    public String llmDescription() {
        return "Toggle whether the trade-route search may include planetary ports ('state').";
    }


    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE,
                "boolean",
                true,
                "Whether planetary ports are included in route calculations. true = include, false = exclude.",
                List.of("true", "false"),
                "Determine whether the commander wants to include or exclude planetary ports."
        );
        state.validate();
        return List.of(state);
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
        boolean isOn = params.get(PARAM_STATE).getAsBoolean();
        TradeProfileManager profileManager = TradeProfileManager.getInstance();
        if(profileManager.setAllowPlanetaryPorts(isOn)) {
            String state = StringUtls.localizedResponse(isOn ? "handler.state.on" : "handler.state.off");
            return StringUtls.localizedResponse("handler.tradeProfile.planetaryPorts", state);
        }
        return null;
    }
}
