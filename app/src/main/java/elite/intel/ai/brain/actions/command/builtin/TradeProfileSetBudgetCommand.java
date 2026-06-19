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
 * Owns its own execution: body migrated 1:1 from the legacy ChangeTradeProfileSetStartingBudgetHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TradeProfileSetBudgetCommand implements IntelCommand {

    private static final List<CustomCommandParameterSpec> PARAMETERS = buildParameters();

    private static List<CustomCommandParameterSpec> buildParameters() {
        CustomCommandParameterSpec key = new CustomCommandParameterSpec(
                "key",
                "number",
                true,
                "Starting budget in credits for the trade route profile.",
                List.of("50000000", "1000000"),
                "Extract the credit amount the commander wants to set as the trade budget."
        );
        key.validate();
        return List.of(key);
    }

    @Override
    public String id() {
        return CommandIds.TRADE_PROFILE_SET_BUDGET;
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
        Integer budget = StringUtls.getIntSafely(params.get("key").getAsString());
        if (budget == null) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.invalidBudget")));
           return;
        }

        TradeProfileManager manager = TradeProfileManager.getInstance();
        if(manager.setStartingCapitol(budget)) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.startingBudget", budget)));
        }
    }
}
