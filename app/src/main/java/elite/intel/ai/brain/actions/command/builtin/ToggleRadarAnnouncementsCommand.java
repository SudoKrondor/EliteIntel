package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.PlayerSession;

import java.util.List;

/**
 * Stage-4b self-describing command for "toggle radar announcements".
 * Behaviour
 * is intentionally minimal (no confirmation announcement), matching the legacy handler 1:1.
 */
@RegisterCommand
public final class ToggleRadarAnnouncementsCommand implements IntelCommand {
    public static final String ID = "toggle_radar_announcements";

    @Override public String llmDescription() { return "Toggle radar contact announcements on or off."; }


    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec state = new ActionParameterSpec(
                "state", "boolean", true,
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
        boolean isOn = params.get("state").getAsBoolean();
        PlayerSession.getInstance().setRadarContactAnnouncementOn(isOn);
    }
}
