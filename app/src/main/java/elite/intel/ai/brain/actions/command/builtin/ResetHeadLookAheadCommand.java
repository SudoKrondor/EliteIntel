package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.inputs.UiNavCommon;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_HEAD_LOOK_RESET;

/**
 * Self-describing "reset head look ahead" command.
 * Owns its own execution: body migrated 1:1 from the legacy LookAheadHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ResetHeadLookAheadCommand implements IntelCommand {
    public static final String ID = "reset_head_look_ahead";

    @Override public String llmDescription() { return "Reset the head-look camera to face forward."; }


    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {

        if (status.isInMainShip()) {
            UiNavCommon.close();
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_HEAD_LOOK_RESET.getGameBinding())));
        }
    }
}
