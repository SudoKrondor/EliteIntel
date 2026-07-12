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
 * Stage-4b self-describing command for "toggle discovery announcements".
 */
@RegisterCommand
public final class ToggleDiscoveryAnnouncementsCommand implements IntelCommand {
    public static final String ID = "toggle_discovery_announcements";

    @Override
    public String llmDescription() {
        return "Turn exploration/discovery-scan announcements on or off ('state').";
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
        String state = StringUtls.localizedLlm(isOn ? "handler.state.on" : "handler.state.off");
        return StringUtls.localizedLlm("handler.announcements.discovery", state);
    }
}
