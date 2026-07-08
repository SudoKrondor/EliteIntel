package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.managers.TradeProfileManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Owns its own execution: body migrated 1:1 from the legacy ChangeTradeProfileSetMaxDistanceFromEntryHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class TradeProfileSetMaxDistanceCommand implements IntelCommand {
    public static final String ID = "trade_profile_set_max_distance";

    @Override
    public String llmDescription() {
        return "Set the trade-route profile's maximum acceptable station distance from arrival (light seconds) to 'key'.";
    }


    private static final String PARAM_KEY = "key";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec key = new ActionParameterSpec(
                PARAM_KEY,
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
    public void execute(JsonObject params, String responseText) {
        Integer distanceFromEntry = StringUtls.getIntSafely(params.get(PARAM_KEY).getAsString());

        if(distanceFromEntry == null){
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.invalidDistance")));
            return;
        }

        TradeProfileManager manager = TradeProfileManager.getInstance();
        if(manager.setDistanceFromSystemEntry(distanceFromEntry)) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.tradeProfile.distanceFromEntry", distanceFromEntry)));
        }
    }
}
