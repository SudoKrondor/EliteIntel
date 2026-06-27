package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_RESET_POWER_DISTRIBUTION;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_RESET_POWER_DISTRIBUTION_BUGGY;

/**
 * Stage-4b self-describing command for "equalize power".
 */
@RegisterCommand
public final class EqualizePowerCommand implements IntelCommand {
    public static final String ID = "equalize_power";

    @Override public String llmDescription() { return "Balance power equally across engines, weapons, and systems."; }


    @Override
    public String id() {
        return ID;
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
