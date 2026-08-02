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
 * Stage-4b self-describing command for "deploy hardpoints".
 */
@RegisterCommand
public final class DeployHardpointsCommand implements IntelCommand {
    public static final String ID = "deploy_hardpoints";

    @Override
    public String llmDescription() {
        return "Deploy (run out) the ship's hardpoints. A hardpoint carries any hardpoint-mounted equipment - "
                + "weapons, but equally mining lasers, limpet controllers and research or scanning gear - so this "
                + "is what the commander needs before using any of it, not only before a fight. "
                + "\"Hardpoints\" on its own is an order to deploy them, never a question about what is installed.";
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

        if (status.isHardpointsDeployed()) {
            return StringUtls.localizedResponse("handler.hardpoints.alreadyDeployed");
        } else {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_HARDPOINTS_TOGGLE.getGameBinding())));
        }
        return null;
    }
}
