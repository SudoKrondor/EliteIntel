package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_EXIT_SUPERCRUISE;

/**
 * Stage-4b self-describing command for "drop from super cruise".
 */
@RegisterCommand
public final class DropFromSuperCruiseCommand implements IntelCommand {
    public static final String ID = "drop_from_super_cruise";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isInSupercruise()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_EXIT_SUPERCRUISE.getGameBinding())));
        }
    }
}
