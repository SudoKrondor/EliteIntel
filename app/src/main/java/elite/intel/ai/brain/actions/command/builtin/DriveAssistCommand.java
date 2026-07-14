package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

import java.util.List;

@RegisterCommand
public final class DriveAssistCommand extends SimpleTapCommand {
    public static final String ID = "drive_assist";
    private static final String PARAM_STATE = "state";
    private static final List<ActionParameterSpec> PARAMETERS = buildParameters();

    @Override
    public String llmDescription() {
        return "Control SRV drive assist (automatic speed/traction control). Omit 'state' to toggle it; use state=true to turn it on or state=false to turn it off.";
    }

    private static List<ActionParameterSpec> buildParameters() {
        ActionParameterSpec state = new ActionParameterSpec(
                PARAM_STATE,
                "boolean",
                false,
                "Desired SRV drive-assist state. Omit it to press the toggle binding.",
                List.of("true", "false"),
                "on/enable/activate -> true; off/disable/deactivate -> false; omit for toggle.");
        state.validate();
        return List.of(state);
    }

    public DriveAssistCommand() {
        super(ID, Bindings.GameCommand.BINDING_DRIVE_ASSIST.getGameBinding());
    }

    /** Returns the optional desired state; omission preserves the physical toggle behavior. */
    @Override
    public List<ActionParameterSpec> parameters() {
        return PARAMETERS;
    }

    /** Applies an explicit state idempotently, or presses the binding once when state is omitted. */
    @Override
    public String execute(JsonObject params, String responseText) {
        JsonElement state = params == null ? null : params.get(PARAM_STATE);
        if (state != null && !state.isJsonNull()) {
            boolean desiredState = state.getAsBoolean();
            if (desiredState == Status.getInstance().isSrvDriveAssist()) {
                return null;
            }
        }
        return super.execute(params, responseText);
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInSrv();
    }
}
