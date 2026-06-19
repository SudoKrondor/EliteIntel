package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_TOGGLE_CARGO_SCOOP;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_TOGGLE_CARGO_SCOOP_BUGGY;

/**
 * Stage-4b self-describing command for "toggle cargo scoop".
 */
@RegisterCommand
public final class ToggleCargoScoopCommand implements IntelCommand {
    public static final String ID = "toggle_cargo_scoop";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isInMainShip()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_TOGGLE_CARGO_SCOOP.getGameBinding())));
        }

        if (status.isInSrv()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_TOGGLE_CARGO_SCOOP_BUGGY.getGameBinding())));
        }
    }
}
