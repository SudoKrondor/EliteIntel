package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_ACTIVATE_COMBAT_MODE;

/**
 * Stage-4b self-describing command for "switch to combat mode".
 */
@RegisterCommand
public final class SwitchToCombatModeCommand implements IntelCommand {
    public static final String ID = "switch_to_combat_mode";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isAnalysisMode()) {
            if (status.isInMainShip()) {
                GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_COMBAT_MODE.getGameBinding())));
            }

            if (status.isInSrv()) {
                GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_ACTIVATE_COMBAT_MODE.getGameBinding())));
            }
        }
    }
}
