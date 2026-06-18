package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandParameterSpec;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ChangeTradeProfileSetAllowProhibitedCargoHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TradeProfileToggleProhibitedCargoCommand implements IntelCommand {

    private static final List<CustomCommandParameterSpec> PARAMETERS = buildParameters();

    private static List<CustomCommandParameterSpec> buildParameters() {
        CustomCommandParameterSpec state = new CustomCommandParameterSpec(
                "state",
                "boolean",
                true,
                "Whether prohibited (illegal) cargo is allowed in route calculations. true = allow, false = disallow.",
                List.of("true", "false"),
                "Determine whether the commander wants to allow or disallow prohibited cargo."
        );
        state.validate();
        return List.of(state);
    }

    @Override
    public String id() {
        return CommandIds.TRADE_PROFILE_TOGGLE_PROHIBITED_CARGO;
    }

    @Override
    public List<CustomCommandParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        boolean isOn = params.get("state").getAsBoolean();
        TradeProfileManager profileManager = TradeProfileManager.getInstance();
        if(profileManager.setAllowProhibitedCargo(isOn)) {
            String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.prohibitedCargo", state)));
        }
    }
}
