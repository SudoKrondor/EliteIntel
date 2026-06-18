package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_SET_SPEED75;

/**
 * Stage-4b self-describing command for "set optimal speed".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy SetOptimalSpeedHandler.
 */
@RegisterCommand
public final class SetOptimalSpeedCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.SET_OPTIMAL_SPEED;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_SET_SPEED75.getGameBinding()))); /// Sets to 75%
    }
}
