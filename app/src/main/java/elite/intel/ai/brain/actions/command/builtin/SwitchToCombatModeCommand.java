package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_ACTIVATE_COMBAT_MODE;

/**
 * Stage-4b self-describing command for "switch to combat mode".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy ActivateCombatModeHandler.
 */
@RegisterCommand
public final class SwitchToCombatModeCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.SWITCH_TO_COMBAT_MODE;
    }

    @Override
    public boolean ownsExecution() {
        return true;
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
