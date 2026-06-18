package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_RESET_POWER_DISTRIBUTION;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_RESET_POWER_DISTRIBUTION_BUGGY;

/**
 * Stage-4b self-describing command for "equalize power".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy ResetPowerSettings.
 */
@RegisterCommand
public final class EqualizePowerCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.EQUALIZE_POWER;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isInMainShip()) {
            String resetPowerDistribution = BINDING_RESET_POWER_DISTRIBUTION.getGameBinding();
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(resetPowerDistribution)));
        }

        if (status.isInSrv()) {
            String resetPowerDistribution = BINDING_RESET_POWER_DISTRIBUTION_BUGGY.getGameBinding();
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(resetPowerDistribution)));
        }
    }
}
