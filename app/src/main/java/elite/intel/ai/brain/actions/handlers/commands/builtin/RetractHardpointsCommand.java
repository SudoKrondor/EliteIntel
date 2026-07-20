package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_HARDPOINTS_TOGGLE;

/**
 * Stage-4b self-describing command for "retract hardpoints".
 */
@RegisterCommand
public final class RetractHardpointsCommand implements IntelCommand {
    public static final String ID = "retract_hardpoints";

    @Override
    public String llmDescription() {
        return "Retract and stow the weapon hardpoints, standing down from combat.";
    }


    @Override
    public String id() {
        return ID;
    }

    /** Hardpoints stay locked in a station's no-fire zone: not while docked. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isInMainShip()) {
            if (status.isHardpointsDeployed()) {
                GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_HARDPOINTS_TOGGLE.getGameBinding())));
            } else {
                return StringUtls.localizedLlm("handler.hardpoints.alreadyRetracted");
            }
        }
        return null;
    }
}
