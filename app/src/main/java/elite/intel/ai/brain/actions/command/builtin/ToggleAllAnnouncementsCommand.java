package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Stage-4b self-describing command for "toggle all announcements".
 * No parameters
 * beyond the LLM PARAM_STATE flag.
 */
@RegisterCommand
public final class ToggleAllAnnouncementsCommand implements IntelCommand {
    public static final String ID = "toggle_all_announcements";

    @Override
    public String llmDescription() {
        return "Turn all spoken announcement categories (discovery, route, radar, mining, navigation) on or off together; 'state' true = on.";
    }


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

    /** App-side announcement setting (no game input); executable in any location. */
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
        if (params.get(PARAM_STATE) == null) {
            return StringUtls.localizedLlm("handler.common.llmParamFailed");
        }
        boolean isOn = params.get(PARAM_STATE).getAsBoolean();
        PlayerSession playerSession = PlayerSession.getInstance();
        playerSession.setDiscoveryAnnouncementOn(isOn);
        playerSession.setRouteAnnouncementOn(isOn);
        playerSession.setRadarContactAnnouncementOn(isOn);
        playerSession.setMiningAnnouncementOn(isOn);
        playerSession.setNavigationAnnouncementOn(isOn);
        String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
        return StringUtls.localizedLlm("handler.announcements.all", state);
    }
}
