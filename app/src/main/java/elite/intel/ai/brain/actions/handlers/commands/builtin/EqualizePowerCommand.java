package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
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

    @Override
    public String llmDescription() {
        return "Reset the power distributor to balanced pips across systems, engines and weapons.";
    }


    @Override
    public String id() {
        return ID;
    }

    /** Pip distribution is disabled while docked; available in flight and in the SRV. */
    ///DO NOT block pip setting in docked mode!!!
    @Override
    public boolean isVisibleForLLM(Status status) {
        return (status.isInMainShip() || status.isInSrv());
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isInMainShip()) {
            String resetPowerDistribution = BINDING_RESET_POWER_DISTRIBUTION.getGameBinding();
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(resetPowerDistribution)));
        }

        if (status.isInSrv()) {
            String resetPowerDistribution = BINDING_RESET_POWER_DISTRIBUTION_BUGGY.getGameBinding();
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(resetPowerDistribution)));
        }
        return null;
    }
}
