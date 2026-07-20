package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;

import java.util.List;

/**
 * Stage-4b self-describing command for "toggle radar announcements".
 * Behaviour
 * is intentionally minimal (no confirmation announcement), matching the legacy handler 1:1.
 */
@RegisterCommand
public final class ToggleRadarAnnouncementsCommand implements IntelCommand {
    public static final String ID = "toggle_radar_announcements";

    @Override
    public String llmDescription() {
        return "Turn radar-contact announcements on or off ('state').";
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
        boolean isOn = params.get(PARAM_STATE).getAsBoolean();
        PlayerSession.getInstance().setRadarContactAnnouncementOn(isOn);
        return null;
    }
}
