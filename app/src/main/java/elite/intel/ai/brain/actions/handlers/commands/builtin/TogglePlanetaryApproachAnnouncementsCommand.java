package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import java.util.List;

/**
 * Self-describing command for "toggle planetary approach announcements".
 *
 * <p>WHY separate from {@link ToggleRouteAnnouncementsCommand}: the approach briefing (gravity,
 * atmosphere, temperature, materials) fires on every body a commander drops towards, which is a very
 * different rhythm from route progress. Silencing one used to silence the other.
 */
@RegisterCommand
public final class TogglePlanetaryApproachAnnouncementsCommand implements IntelCommand {
    public static final String ID = "toggle_planetary_approach_announcements";

    @Override
    public String llmDescription() {
        return "Turn planetary approach announcements - the gravity, atmosphere and temperature briefing "
                + "given when approaching a planet or moon - on or off ('state').";
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

    /**
     * App-side announcement setting (no game input); executable in any location.
     */
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
            return StringUtls.localizedResponse("handler.common.llmParamFailed");
        }
        boolean isOn = params.get(PARAM_STATE).getAsBoolean();
        PlayerSession.getInstance().setPlanetaryApproachAnnouncementOn(isOn);
        String state = StringUtls.localizedResponse(isOn ? "handler.state.on" : "handler.state.off");
        return StringUtls.localizedResponse("handler.announcements.planetaryApproach", state);
    }
}
