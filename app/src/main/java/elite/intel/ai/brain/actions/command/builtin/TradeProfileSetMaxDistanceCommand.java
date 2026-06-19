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
 * Owns its own execution: body migrated 1:1 from the legacy ChangeTradeProfileSetMaxDistanceFromEntryHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TradeProfileSetMaxDistanceCommand implements IntelCommand {

    private static final List<CustomCommandParameterSpec> PARAMETERS = buildParameters();

    private static List<CustomCommandParameterSpec> buildParameters() {
        CustomCommandParameterSpec key = new CustomCommandParameterSpec(
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
        return CommandIds.TRADE_PROFILE_SET_MAX_DISTANCE;
    }

    @Override
    public List<CustomCommandParameterSpec> parameters() {
        return PARAMETERS;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Integer distanceFromEntry = StringUtls.getIntSafely(params.get("key").getAsString());

        if(distanceFromEntry == null){
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.invalidDistance")));
            return;
        }

        TradeProfileManager manager = TradeProfileManager.getInstance();
        if(manager.setDistanceFromSystemEntry(distanceFromEntry)) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.distanceFromEntry", distanceFromEntry)));
        }
    }
}
