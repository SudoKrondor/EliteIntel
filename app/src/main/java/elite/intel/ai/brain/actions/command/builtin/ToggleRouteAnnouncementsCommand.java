package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.PlayerSession;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Stage-4b self-describing command for "toggle route announcements".
 */
@RegisterCommand
public final class ToggleRouteAnnouncementsCommand implements IntelCommand {
    public static final String ID = "toggle_route_announcements";

    @Override public String llmDescription() { return "Toggle route and jump announcements on or off."; }


    private static final String PARAM_STATE = "state";

    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE, "boolean", true,
                "Whether to turn it on (true) or off (false).",
                List.of("true", "false"),
                "on/enable/activate → true; off/disable/deactivate → false.");
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
    public void execute(JsonObject params, String responseText) {
        boolean isOn = params.get(PARAM_STATE).getAsBoolean();
        PlayerSession playerSession = PlayerSession.getInstance();
        playerSession.setRouteAnnouncementOn(isOn);
        String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.announcements.route", state)));
    }
}
