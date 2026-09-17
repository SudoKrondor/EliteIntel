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
 * "Announce cargo-scoop pickups on/off". Silences the running commentary on what the scoop just
 * swallowed (materials, today); the ledger keeps being written either way. Deliberately not a
 * sibling of {@code toggle_cargo_scoop}, which opens and closes the scoop itself - the phrases
 * all carry "announce"/"pickup" wording so the two never share a training phrase.
 */
@RegisterCommand
public final class ToggleCargoScoopPickupAnnouncementsCommand implements IntelCommand {
    public static final String ID = "toggle_cargo_scoop_pickup_announcements";

    @Override
    public String llmDescription() {
        return "Turn spoken cargo-scoop pickup announcements (what was just scooped) on or off ('state'). Does not open or close the scoop.";
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
        PlayerSession.getInstance().setCargoScoopPickupAnnouncementOn(isOn);
        String state = StringUtls.localizedResponse(isOn ? "handler.state.on" : "handler.state.off");
        return StringUtls.localizedResponse("handler.announcements.cargoScoopPickup", state);
    }
}
